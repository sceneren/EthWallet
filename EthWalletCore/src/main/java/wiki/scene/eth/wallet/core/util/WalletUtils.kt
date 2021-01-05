package wiki.scene.eth.wallet.core.util

import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.github.novacrypto.hashing.Sha256
import io.reactivex.Observable
import org.web3j.crypto.*
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import wiki.scene.eth.wallet.core.bean.WalletInfo
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.manager.OtherWalletDBManager
import wiki.scene.eth.wallet.core.db.manager.WalletInfoTableDBManager
import wiki.scene.eth.wallet.core.db.table.WalletInfoTable
import wiki.scene.eth.wallet.core.exception.WalletException
import wiki.scene.eth.wallet.core.exception.WalletExceptionCode
import wiki.scene.eth.wallet.core.ext.changeIOThread
import java.io.File
import java.math.BigInteger
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
                it.onComplete()
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
                val walletInfo = WalletInfoTable(walletName, walletType.ordinal, mnemonic, realPrivateKey, publicKey, if (walletType == WalletType.ETH_WALLET_TYPE_ETH) getETHAddress(credentials.address) else getSETAddress(credentials.address), walletPassword)
                //写入本地数据库
                val result = WalletInfoTableDBManager.insertOrUpdateWallet(walletInfo, 1, walletListImageRes)
                if (result) {
                    //设置默认
                    return@flatMap Observable.just(WalletInfoTableDBManager.setDefaultWalletByLast())
                } else {
                    return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_DATABASE))
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
        return Observable.create<MutableList<String>> {
            if (mnemonic.isNotEmpty()) {
                val mnemonicList = mnemonic.split(" ")
                if (mnemonicList.size != 12) {
                    it.onError(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
                } else {
                    it.onNext(mnemonicList.toMutableList())
                    it.onComplete()
                }
            } else {
                it.onError(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
            }
        }.flatMap {
            //判断钱包是否存在
            val walletInfoTable = WalletInfoTableDBManager.queryWalletByMnemonic(mnemonic)
            if (walletInfoTable == null) {
                Observable.just(it)
            } else {
                Observable.error(WalletException(WalletExceptionCode.ERROR_WALLET_EXITS))
            }
        }.flatMap { mnemonicList ->
            //查询是否删除的钱包
            val otherWalletInfo = OtherWalletDBManager.queryWalletByMnemonic(mnemonic)
            val walletInfo = if (otherWalletInfo != null) {
                //删除过的钱包直接从本地恢复
                val address = otherWalletInfo.address
                val privateKey = otherWalletInfo.sy
                val publicKey = otherWalletInfo.gy
                val realMnemonic = otherWalletInfo.mn
                WalletInfoTable(walletName, walletType.ordinal, realMnemonic, privateKey, publicKey, if (walletType == WalletType.ETH_WALLET_TYPE_ETH) getETHAddress(address) else getSETAddress(address), walletPassword)
            } else {
                //未删除过的直接创建
                val seed = SeedCalculator()
                        .withWordsFromWordList(English.INSTANCE)
                        .calculateSeed(mnemonicList, walletPassword)
                val ecKeyPair = ECKeyPair.create(Sha256.sha256(seed))
                val privateKey = ecKeyPair.privateKey.toString(16)
                val publicKey = ecKeyPair.publicKey.toString(16)
                WalletUtils.generateWalletFile(walletPassword, ecKeyPair, File(EthWalletCore.getWalletFilePath()), false)
                val address = Keys.getAddress(publicKey)
                WalletInfoTable(walletName, walletType.ordinal, mnemonic, privateKey, publicKey, if (walletType == WalletType.ETH_WALLET_TYPE_ETH) getETHAddress(address) else getSETAddress(address), walletPassword)
            }
            return@flatMap Observable.just(walletInfo)
        }.flatMap { walletInfo ->
            //写入本地数据库
            val result = WalletInfoTableDBManager.insertOrUpdateWallet(walletInfo, 1, walletListImageRes)
            if (result) {
                //设置默认
                return@flatMap Observable.just(WalletInfoTableDBManager.setDefaultWalletByLast())
            } else {
                return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_DATABASE))
            }
        }.changeIOThread()

    }

    /**
     * 根据私钥导入钱包
     */
    fun importWalletByPrivateKey(walletType: WalletType, walletName: String, walletPassword: String, privateKey: String, walletListImageRes: Int): Observable<Boolean> {
        return Observable.create<Boolean> {
            if (privateKey.isNotEmpty()) {
                if (WalletUtils.isValidPrivateKey(privateKey)) {
                    it.onNext(true)
                    it.onComplete()
                } else {
                    it.onError(WalletException(WalletExceptionCode.ERROR_PRIVATE_KEY))
                }
            } else {
                it.onError(WalletException(WalletExceptionCode.ERROR_PRIVATE_KEY))
            }
        }.flatMap {
            //判断钱包是否存在
            val walletInfoTable = WalletInfoTableDBManager.queryWalletByPrivateKey(privateKey)
            if (walletInfoTable == null) {
                Observable.just(true)
            } else {
                Observable.error(WalletException(WalletExceptionCode.ERROR_WALLET_EXITS))
            }
        }.flatMap {
            //查询是否删除的钱包
            val otherWalletInfo = OtherWalletDBManager.queryWalletByPrivateKey(privateKey)
            val walletInfo = if (otherWalletInfo != null) {
                //删除过的钱包直接从本地恢复
                val address = otherWalletInfo.address
                val realPrivateKey = otherWalletInfo.sy
                val publicKey = otherWalletInfo.gy
                val mnemonic = otherWalletInfo.mn
                WalletInfoTable(walletName, walletType.ordinal, mnemonic, realPrivateKey, publicKey, if (walletType == WalletType.ETH_WALLET_TYPE_ETH) getETHAddress(address) else getSETAddress(address), walletPassword)
            } else {
                //未删除过的直接创建
                val credentials = Credentials.create(privateKey)
                val ecKeyPair: ECKeyPair = credentials.ecKeyPair
                KeyStoreUtils.genKeyStore2Files(walletPassword, ecKeyPair)
                val address = credentials.address
                val realPrivateKey = Numeric.encodeQuantity(ecKeyPair.privateKey)
                val publicKey = Numeric.encodeQuantity(ecKeyPair.publicKey)
                WalletInfoTable(walletName, walletType.ordinal, "", realPrivateKey, publicKey, if (walletType == WalletType.ETH_WALLET_TYPE_ETH) getETHAddress(address) else getSETAddress(address), walletPassword)
            }
            return@flatMap Observable.just(walletInfo)
        }.flatMap { walletInfo ->
            //写入本地数据库
            val result = WalletInfoTableDBManager.insertOrUpdateWallet(walletInfo, 1, walletListImageRes)
            if (result) {
                //设置默认
                return@flatMap Observable.just(WalletInfoTableDBManager.setDefaultWalletByLast())
            } else {
                return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_DATABASE))
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
     * 获取所有的钱包列表
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
                it.onComplete()
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
                it.onNext(privateKey)
                it.onComplete()
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
                it.onNext(mnemonic)
                it.onComplete()
            } catch (e: WalletException) {
                e.printStackTrace()
                it.onError(e)
            } catch (e: Exception) {
                e.printStackTrace()
                it.onError(WalletException(WalletExceptionCode.ERROR_UNKNOWN))
            }
        }.changeIOThread()
    }

    fun transaction(password: String, walletId: Long, toAddress: String) {
        Observable.create<WalletInfo> {
            val walletInfo = WalletInfoTableDBManager.queryWalletByWalletId(walletId)
            if (walletInfo == null) {
                it.onError(WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND))
            } else {
                it.onNext(walletInfo)
            }
        }.flatMap { walletInfo ->
            try {
                val web3j = Web3j.build(HttpService("https://mainnet.infura.io/"))
                //获取nonce
                val ethGetTransactionCount = web3j.ethGetTransactionCount(walletInfo.walletAddress, DefaultBlockParameterName.PENDING).sendAsync().get()
                val nonce = ethGetTransactionCount.transactionCount

                val rawTransaction = RawTransaction.createEtherTransaction(
                        nonce, Convert.toWei("18", Convert.Unit.GWEI).toBigInteger(),
                        Convert.toWei("45000", Convert.Unit.WEI).toBigInteger(), toAddress, BigInteger("3000000000000000000"))


                return@flatMap Observable.just("")
            } catch (e: Exception) {
                e.printStackTrace()
                return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_UNKNOWN))
            }
        }.changeIOThread()

    }


    /**
     * 校验是否是SET地址
     */
    fun isSETValidAddress(input: String?): Boolean {
        return if (input == null || !input.startsWith("SET")) false else input.length == 36
    }

    /**
     * ETH地址转SET地址
     */
    fun getSETAddress(ethAddress: String): String {
        if (isSETValidAddress(ethAddress)) {
            return ethAddress
        } else {
            return if(ethAddress.startsWith("0x")){
                "SET" + Base58.encode(ethAddress.substring(2, 26).toByteArray())
            }else{
                "SET" + Base58.encode(ethAddress.substring(0, 24).toByteArray())
            }
        }
    }

    fun isETHValidAddress(input: String?): Boolean {
        return if (input == null || !input.startsWith("0x")) false else WalletUtils.isValidAddress(input)
    }

    fun getETHAddress(address: String): String {
        if (isETHValidAddress(address)) {
            return address
        } else {
            return "0x$address"
        }
    }

}