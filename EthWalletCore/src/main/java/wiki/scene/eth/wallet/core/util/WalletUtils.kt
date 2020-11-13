package wiki.scene.eth.wallet.core.util

import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.github.novacrypto.hashing.Sha256
import io.reactivex.Observable
import org.web3j.crypto.*
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric
import wiki.scene.eth.wallet.core.bean.WalletInfo
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.manager.WalletInfoTableDBManager
import wiki.scene.eth.wallet.core.db.table.WalletInfoTable
import wiki.scene.eth.wallet.core.exception.WalletException
import wiki.scene.eth.wallet.core.exception.WalletExceptionCode
import wiki.scene.eth.wallet.core.ext.changeIOThread
import java.io.File
import java.security.SecureRandom


object WalletUtils {
    /**
     * 检查是否有钱包
     */
    fun hasWallet(): Observable<Boolean> {
        return Observable.just(WalletInfoTableDBManager.queryWalletSize() > 0L)
                .changeIOThread()
    }

    /**
     * 创建助记词
     */
    fun createMnemonic(): Observable<MutableList<String>> {
        return Observable.create<MutableList<String>> {
            val sb = StringBuilder()
            val entropy = ByteArray(Words.TWELVE.byteLength())
            SecureRandom().nextBytes(entropy)
            MnemonicGenerator(English.INSTANCE).createMnemonic(entropy) { charSequence: CharSequence? ->
                if (!charSequence.isNullOrEmpty()) {
                    sb.append(charSequence)
                }
            }
            val mnemonicList = sb.toString().split(" ").toMutableList()
            if (mnemonicList.size != 12) {
                it.onError(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
            } else {
                it.onNext(mnemonicList)
                it.onComplete()
            }

        }.changeIOThread()

    }

    /**
     * 校验助记词
     */
    fun veryMnemonic(inputMnemonicList: MutableList<String>, mnemonicList: MutableList<String>): Observable<Boolean> {
        return Observable.create<Boolean> {
            if (inputMnemonicList.size != 12 || mnemonicList.size != 12) {
                it.onError(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
            } else {
                val inputMnemonicListStr = inputMnemonicList.joinToString(" ")
                val mnemonicListStr = mnemonicList.joinToString(" ")
                it.onNext(inputMnemonicListStr == mnemonicListStr)
            }
        }.changeIOThread()
    }

    /**
     * @param walletName 钱包名称
     * @param walletPassword 钱包密码
     * @param mnemonic 助记词
     */
    fun createWallet(walletType: WalletType, walletName: String, walletPassword: String, mnemonic: String, walletListImageRes: Int): Observable<Boolean> {
        return Observable.create<File> {
            //创建钱包目录
            try {
                val fileDir = File(EthWalletCore.getWalletFilePath())
                val result = if (!fileDir.exists()) {
                    fileDir.mkdirs()
                } else {
                    true
                }
                if (result) {
                    it.onNext(fileDir)
                    it.onComplete()
                } else {
                    it.onError(WalletException(WalletExceptionCode.ERROR_CREATE_WALLET_FILE_FAIL))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                it.onError(WalletException(WalletExceptionCode.ERROR_PERMISSION_DEFINE))
            }
        }.flatMap { fileDir ->
            //创建钱包
            val seed = MnemonicUtils.generateSeed(mnemonic, walletPassword)
            val privateKey = ECKeyPair.create(Sha256.sha256(seed))
            val walletFile: String
            try {
                walletFile = WalletUtils.generateWalletFile(walletPassword, privateKey, fileDir, false)
                val bip39Wallet = Bip39Wallet(walletFile, mnemonic)
                val credentials = WalletUtils.loadBip39Credentials(walletPassword, bip39Wallet.mnemonic)
                val realPrivateKey = Numeric.encodeQuantity(credentials.ecKeyPair.privateKey)
                val publicKey = Numeric.encodeQuantity(credentials.ecKeyPair.publicKey)
                val walletInfo = WalletInfoTable(walletName, walletType.ordinal, mnemonic, realPrivateKey, publicKey, credentials.address, walletPassword)
                //写入本地数据库
                val result = WalletInfoTableDBManager.insertOrUpdateWallet(walletInfo, 1, walletListImageRes)
                if (result) {
                    //设置默认
                    Observable.just(WalletInfoTableDBManager.setDefaultWalletByLast())
                } else {
                    Observable.error(WalletException(WalletExceptionCode.ERROR_DATABASE))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_CREATE_WALLET_FAIL))
            }
        }.changeIOThread()

    }

    /**
     * 根据助记词导入钱包
     */
    fun importWalletByMnemonic(walletType: WalletType, walletName: String, walletPassword: String, mnemonic: String, walletListImageRes: Int): Observable<Boolean> {
        return Observable.create<Boolean> {
            if (mnemonic.isEmpty()) {
                it.onNext(true)
                it.onComplete()
            } else {
                it.onError(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
            }
        }.flatMap {
            return@flatMap Observable.just(mnemonic.split(" "))
        }.flatMap { mnemonicList ->
            if (mnemonicList.size != 12) {
                Observable.error(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
            } else {
                try {
                    val seed = SeedCalculator()
                            .withWordsFromWordList(English.INSTANCE)
                            .calculateSeed(mnemonicList, walletPassword)
                    val ecKeyPair = ECKeyPair.create(Sha256.sha256(seed))
                    val privateKey = ecKeyPair.privateKey.toString(16)
                    val publicKey = ecKeyPair.publicKey.toString(16)
                    WalletUtils.generateWalletFile(walletPassword, ecKeyPair, File(EthWalletCore.getWalletFilePath()), false)
                    val walletInfo = WalletInfoTable(walletName, walletType.ordinal, mnemonic, privateKey, publicKey, Keys.getAddress(publicKey), walletPassword)
                    //写入本地数据库
                    val result = WalletInfoTableDBManager.insertOrUpdateWallet(walletInfo, 1, walletListImageRes)
                    if (result) {
                        //设置默认
                        Observable.just(WalletInfoTableDBManager.setDefaultWalletByLast())
                    } else {
                        Observable.error(WalletException(WalletExceptionCode.ERROR_DATABASE))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Observable.error(WalletException(WalletExceptionCode.ERROR_CREATE_WALLET_FAIL))
                }

            }
        }.changeIOThread()
    }

    /**
     * 根据私钥导入钱包
     */
    fun importWalletByPrivateKey(walletType: WalletType, walletName: String, walletPassword: String, privateKey: String, walletListImageRes: Int): Observable<Boolean> {
        return Observable.create<Boolean> {
            if (privateKey.isEmpty()) {
                it.onNext(true)
                it.onComplete()
            } else {
                it.onError(WalletException(WalletExceptionCode.ERROR_PRIVATE_KEY))
            }
        }.flatMap {
            val credentials = Credentials.create(privateKey)
            val ecKeyPair: ECKeyPair = credentials.ecKeyPair
            KeyStoreUtils.genKeyStore2Files(walletPassword, ecKeyPair)
            val address = credentials.address
            val realPrivateKey = Numeric.encodeQuantity(ecKeyPair.privateKey)
            val publicKey = Numeric.encodeQuantity(ecKeyPair.publicKey)
            val walletInfo = WalletInfoTable(walletName, walletType.ordinal, "", realPrivateKey, publicKey, address, walletPassword)
            //写入本地数据库
            val result = WalletInfoTableDBManager.insertOrUpdateWallet(walletInfo, 1, walletListImageRes)
            if (result) {
                //设置默认
                Observable.just(WalletInfoTableDBManager.setDefaultWalletByLast())
            } else {
                Observable.error(WalletException(WalletExceptionCode.ERROR_DATABASE))
            }
        }.changeIOThread()
    }

    /**
     * 根据类型获取钱包列表
     */
    fun getWalletListByType(walletType: WalletType): Observable<MutableList<WalletInfo>> {
        return Observable.just(WalletInfoTableDBManager.queryWalletByType(walletType.ordinal))
                .changeIOThread()
    }

    /**
     * 获取所以的钱包列表
     */
    fun getWalletList(): Observable<MutableList<WalletInfo>> {
        return Observable.just(WalletInfoTableDBManager.queryWallet())
                .changeIOThread()
    }

    /**
     * 获取默认钱包
     */
    fun getDefaultWallet(): Observable<WalletInfo> {
        return Observable.create<WalletInfo> {
            val defaultWalletInfo = WalletInfoTableDBManager.queryDefaultWallet()
            if (defaultWalletInfo != null) {
                it.onNext(defaultWalletInfo)
                it.onComplete()
            } else {
                it.onError(WalletException(WalletExceptionCode.ERROR_NOT_FIND_DEFAULT_WALLET))
            }
        }.changeIOThread()
    }

    /**
     * 设置默认钱包
     */
    fun setDefaultWallet(walletId: Long): Observable<Boolean> {
        return Observable.just(WalletInfoTableDBManager.setDefaultWalletByWalletId(walletId))
                .changeIOThread()
    }

    /**
     * 修改钱包名称
     */
    fun updateWalletName(walletId: Long, newWalletName: String): Observable<Boolean> {
        return Observable.create<Boolean> {
            try {
                it.onNext(WalletInfoTableDBManager.updateWalletName(walletId, newWalletName))
            } catch (e: WalletException) {
                it.onError(e)
            }
        }.changeIOThread()
    }

    /**
     * 删除钱包
     */
    fun deleteWalletById(walletId: Long, password: String): Observable<Boolean> {
        return Observable.create<Boolean> {
            try {
                val result = WalletInfoTableDBManager.deleteWalletByWalletId(walletId, password)
                it.onNext(result)
                it.onComplete()
            } catch (e: WalletException) {
                e.printStackTrace()
                it.onError(e)
            }
        }.changeIOThread()
    }

    /**
     * 导出私钥
     */
    fun exportPrivateKey(walletId: Long, walletPassword: String): Observable<String> {
        return Observable.create<String> {
            try {
                val privateKey = WalletInfoTableDBManager.getPrivateKey(walletId, walletPassword)
                Observable.just(privateKey)
            } catch (e: WalletException) {
                e.printStackTrace()
                it.onError(e)
            } catch (e: Exception) {
                e.printStackTrace()
                it.onError(WalletException(WalletExceptionCode.ERROR_UNKNOWN))
            }
        }.changeIOThread()
    }

    /**
     * 导出助记词
     */
    fun exportMnemonic(walletId: Long, walletPassword: String): Observable<String> {
        return Observable.create<String> {
            try {
                val mnemonic = WalletInfoTableDBManager.getMnemonic(walletId, walletPassword)
                Observable.just(mnemonic)
            } catch (e: WalletException) {
                e.printStackTrace()
                it.onError(e)
            } catch (e: Exception) {
                e.printStackTrace()
                it.onError(WalletException(WalletExceptionCode.ERROR_UNKNOWN))
            }
        }.changeIOThread()
    }

}