package wiki.scene.eth.wallet.core.util


import io.reactivex.Observable
import org.consenlabs.tokencore.foundation.utils.MnemonicUtil
import org.consenlabs.tokencore.wallet.model.ChainType
import org.consenlabs.tokencore.wallet.model.Metadata
import org.consenlabs.tokencore.wallet.model.TokenException
import wiki.scene.eth.wallet.core.bean.MyWallet
import wiki.scene.eth.wallet.core.config.WalletConfig
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.table.MyWalletTable
import wiki.scene.eth.wallet.core.exception.WalletException
import wiki.scene.eth.wallet.core.exception.WalletExceptionCode
import wiki.scene.eth.wallet.core.ext.changeIOThread
import wiki.scene.eth.wallet.core.manager.MyWalletTableManager

/**
 *
 * @Description:    ETH钱包工具类
 * @Author:         scene
 * @CreateDate:     2020/10/23 10:52
 * @UpdateUser:     更新者：
 * @UpdateDate:     2020/10/23 10:52
 * @UpdateRemark:   更新说明：
 * @Version:        1.0.0
 */
object EthWalletUtils {

    private fun getIdentity(walletType: WalletType): Observable<Identity> {
        return Observable.create<Identity> {
            val currentIdentity = Identity.getCurrentIdentity(walletType)
            if (currentIdentity == null) {
                val identity = Identity.createIdentity(walletType, walletType.name, "", "", WalletConfig.ETH_NET_WORK, Metadata.P2WPKH)
                it.onNext(identity)
            } else {
                it.onNext(currentIdentity)
            }
        }.changeIOThread()


    }

    /**
     * 是否创建钱包
     */
    fun hasWallet(): Observable<Boolean> {
        return getWalletList().flatMap {
            return@flatMap Observable.just(it.size != 0)
        }.changeIOThread()
    }

    /**
     * 获取所有的钱包列表
     */
    fun getWalletList(): Observable<MutableList<MyWallet>> {
        return Observable.zip(getWalletListByType(WalletType.ETH_WALLET_TYPE_ETH), getWalletListByType(WalletType.ETH_WALLET_TYPE_SET), { ethWalletList, setWalletList ->
            val allWalletList = mutableListOf<MyWallet>()
            allWalletList.addAll(setWalletList)
            allWalletList.addAll(ethWalletList)
            allWalletList.sortBy { it.wallet.id }
            return@zip allWalletList
        }).changeIOThread()
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
     * 创建助记词
     */
    fun createMnemonic(): Observable<MutableList<String>> {
        return Observable.just(MnemonicUtil.randomMnemonicCodes())
                .flatMap {
                    if (it.size == 12) {
                        return@flatMap Observable.just(it)
                    } else {
                        return@flatMap createMnemonic()
                    }
                }.changeIOThread()
    }

    /**
     * 创建钱包
     * @param walletName 钱包名称
     * @param walletPassword 钱包密码
     */
    fun createEthWallet(walletType: WalletType, mnemonicList: MutableList<String>, walletName: String, walletPassword: String, walletListImageRes: Int): Observable<MyWallet> {

        return Observable.just(mnemonicList)
                .flatMap {
                    if (it.size == 12) {
                        return@flatMap Observable.just(it.joinToString(" "))
                    } else {
                        return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
                    }
                }
                .flatMap { importWalletByMnemonic(walletType, it, walletName, walletPassword, walletListImageRes) }
                .flatMap { myWallet ->
                    myWallet.walletDefault = 1
                    return@flatMap Observable.just(myWallet)
                }.changeIOThread()

    }

    /**
     * 根据类型获取钱包列表
     * @param walletType 钱包类型
     */
    fun getWalletListByType(walletType: WalletType): Observable<MutableList<MyWallet>> {
        return getIdentity(walletType).flatMap {
            try {
                val myWalletList = mutableListOf<MyWallet>()
                it.getWallets(walletType).forEach { wallet ->
                    val data = MyWalletTableManager.queryWalletByWalletId(wallet.id)
                    val walletName = data?.walletName ?: ""
                    val walletDefault = data?.walletDefault ?: 0
                    val walletListImageRes = data?.walletListImageRes ?: 0
                    myWalletList.add(MyWallet(wallet, walletType, walletName, walletDefault, walletListImageRes))
                }
                return@flatMap Observable.just(myWalletList)
            } catch (e: TokenException) {
                return@flatMap Observable.error(e)
            }

        }.changeIOThread()
    }

    /**
     * 根据助记词导入钱包
     * @param walletType 钱包类型
     * @param mnemonic 助记词
     * @param walletName 钱包名称
     * @param walletPassword 钱包密码
     */
    fun importWalletByMnemonic(walletType: WalletType, mnemonic: String, walletName: String, walletPassword: String, walletListImageRes: Int): Observable<MyWallet> {
        return getIdentity(walletType)
                .flatMap {
                    try {
                        val mnemonicList = mnemonic.split(" ").toMutableList()
                        if (mnemonicList.size != 12) {
                            return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
                        } else {
                            val metadata = Metadata()
                            metadata.source = Metadata.FROM_MNEMONIC
                            metadata.network = WalletConfig.ETH_NET_WORK
                            metadata.segWit = Metadata.P2WPKH
                            metadata.chainType = ChainType.ETHEREUM
                            val wallet = WalletManager.importWalletFromMnemonic(walletType, metadata, mnemonic, "m/44'/60'/0'/0/0", walletPassword, true)

                            val myWallet = MyWallet(wallet, walletType, walletName, 1, walletListImageRes)
                            val myWalletTable = MyWalletTable(myWallet.wallet.id, walletName, walletType.ordinal)
                            val oldDefaultWallet = MyWalletTableManager.queryDefaultWallet()
                            if (oldDefaultWallet != null) {
                                oldDefaultWallet.walletDefault = 0
                                MyWalletTableManager.insertOrUpdateWallet(oldDefaultWallet)
                            }
                            MyWalletTableManager.insertOrUpdateWallet(myWalletTable, 1, walletListImageRes)
                            return@flatMap Observable.just(myWallet)
                        }
                    } catch (e: TokenException) {
                        return@flatMap Observable.error(e)
                    }
                }
                .changeIOThread()
    }

    /**
     * 根据私钥导入钱包
     *
     * @param walletType 钱包类型
     * @param privateKey 私钥
     * @param walletName 钱包名称
     * @param walletPassword 钱包密码
     */
    fun importWalletByPrivateKey(walletType: WalletType, privateKey: String, walletName: String, walletPassword: String, walletListImageRes: Int): Observable<MyWallet> {
        return getIdentity(walletType)
                .flatMap {
                    try {
                        val metadata = Metadata()
                        metadata.source = Metadata.FROM_MNEMONIC
                        metadata.network = WalletConfig.ETH_NET_WORK
                        metadata.segWit = Metadata.P2WPKH
                        metadata.chainType = ChainType.ETHEREUM
                        val wallet = WalletManager.importWalletFromPrivateKey(walletType, metadata, privateKey, walletPassword, true)
                        val myWalletTable = MyWalletTable(wallet.id, walletName, walletType.ordinal)

                        MyWalletTableManager.insertOrUpdateWallet(myWalletTable, 1, walletListImageRes)

                        return@flatMap Observable.just(MyWallet(wallet, walletType, walletName, 1, walletListImageRes))
                    } catch (e: TokenException) {
                        return@flatMap Observable.error(e)
                    }
                }.changeIOThread()
    }

    /**
     * 删除钱包根据Id
     */
    fun deleteWalletById(walletType: WalletType, walletId: String, password: String): Observable<Boolean> {

        return Observable.just(WalletManager.removeWallet(walletType, walletId, password))
                .flatMap {
                    return@flatMap MyWalletTableManager.deleteWalletByWalletId(walletId)
                }.changeIOThread()

    }


    /**
     * 获取当前钱包的私钥
     */
    fun exportPrivateKey(walletId: String, walletPassword: String): Observable<String> {
        return Observable.create<String> {
            try {
                it.onNext(WalletManager.exportKeystore(walletId, walletPassword))
            } catch (e: TokenException) {
                it.onError(e)
            }
        }.changeIOThread()
    }

    /**
     * 获取当前钱包的助记词
     * @param walletId 钱包Id
     * @param  walletPassword 钱包密码
     */
    fun exportWalletMnemonic(walletId: String, walletPassword: String): Observable<MutableList<String>> {
        return Observable.create<MutableList<String>> {
            try {
                val mnemonic = WalletManager.exportMnemonic(walletId, walletPassword)
                val mnemonicList = mnemonic.mnemonic.split(" ").toMutableList()
                if (mnemonicList.size != 12) {
                    it.onError(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
                } else {
                    it.onNext(mnemonicList)
                }
            } catch (e: TokenException) {
                it.onError(e)
            }

        }.changeIOThread()
    }

    /**
     * 设置默认钱包
     * @param walletId 钱包Id
     */
    fun setDefaultWalletById(walletId: String): Observable<Boolean> {
        return Observable.create<Boolean> {
            val oldDefault = MyWalletTableManager.queryDefaultWallet()
            if (oldDefault != null) {
                MyWalletTableManager.insertOrUpdateWallet(oldDefault, 0, oldDefault.walletListImageRes)
            }
            val data = MyWalletTableManager.queryWalletByWalletId(walletId)
            if (data != null) {
                val result = MyWalletTableManager.insertOrUpdateWallet(data, 1, data.walletListImageRes)
                it.onNext(result)
            } else {
                it.onNext(false)
            }
        }.changeIOThread()

    }

    /**
     * 设置默认钱包
     */
    fun setDefaultWalletByWalletId(walletId: String): Observable<Boolean> {
        return Observable.create<Boolean> {
            val oldDefault = MyWalletTableManager.queryDefaultWallet()
            if (oldDefault != null) {
                MyWalletTableManager.insertOrUpdateWallet(oldDefault, 0, oldDefault.walletListImageRes)
            }
            it.onNext(MyWalletTableManager.setDefaultWalletByWalletId(walletId))
        }.changeIOThread()
    }

    /**
     * 获取默认钱包
     */
    fun getDefaultWallet(): Observable<MyWallet> {
        return Observable.create<MyWallet> {
            val defaultWallet = MyWalletTableManager.queryDefaultWallet()
            if (defaultWallet == null) {
                it.onError(WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND))
            } else {
                try {
                    val wallet = WalletManager.mustFindWalletById(defaultWallet.walletId)
                    it.onNext(MyWallet(wallet, defaultWallet.walletType, defaultWallet.walletName, defaultWallet.walletDefault, defaultWallet.walletListImageRes))
                } catch (e: TokenException) {
                    it.onError(e)
                }
            }
        }.changeIOThread()
    }

    /**
     * 修改钱包名称
     * @param walletId 钱包Id
     * @param walletName 钱包的新名称
     */
    fun updateWalletName(walletId: String, walletName: String): Observable<Boolean> {
        return Observable.create<MyWalletTable> {
            val wallet = MyWalletTableManager.queryWalletByWalletId(walletId)
            if (wallet == null) {
                it.onError(WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND))
            } else {
                it.onNext(wallet)
            }
        }.flatMap {
            it.walletName = walletName
            return@flatMap Observable.just(MyWalletTableManager.insertOrUpdateWallet(it))
        }.changeIOThread()
    }


}