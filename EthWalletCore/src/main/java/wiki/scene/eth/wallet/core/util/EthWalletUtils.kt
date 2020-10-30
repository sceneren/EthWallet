package wiki.scene.eth.wallet.core.util


import io.reactivex.Observable
import org.consenlabs.tokencore.foundation.utils.MnemonicUtil
import org.consenlabs.tokencore.wallet.model.ChainType
import org.consenlabs.tokencore.wallet.model.Metadata
import org.consenlabs.tokencore.wallet.model.TokenException
import wiki.scene.eth.wallet.core.bean.MyWallet
import wiki.scene.eth.wallet.core.config.WalletConfig
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.MyWalletTable
import wiki.scene.eth.wallet.core.db.table.MyWalletTable_
import wiki.scene.eth.wallet.core.exception.WalletException
import wiki.scene.eth.wallet.core.exception.WalletExceptionCode
import wiki.scene.eth.wallet.core.ext.changeIOThread

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
        }
    }

    /**
     * 获取所有的钱包列表
     */
    fun getWalletList(): Observable<MutableList<MyWallet>> {
        return Observable.zip(getIdentity(WalletType.ETH_WALLET_TYPE_SET), getIdentity(WalletType.ETH_WALLET_TYPE_ETH), { setIdentity, ethIdentity ->
            val myWalletList = mutableListOf<MyWallet>()
            val setWalletList = setIdentity.getWallets(WalletType.ETH_WALLET_TYPE_SET).toMutableList()
            val ethWalletList = ethIdentity.getWallets(WalletType.ETH_WALLET_TYPE_ETH).toMutableList()
            setWalletList.forEach {
                myWalletList.add(MyWallet(it, WalletType.ETH_WALLET_TYPE_SET, ""))
            }
            ethWalletList.forEach {
                myWalletList.add(MyWallet(it, WalletType.ETH_WALLET_TYPE_ETH, ""))
            }

            myWalletList.forEach {
                val result = ObjectBox.getMyWalletTableManager()
                        .query()
                        .equal(MyWalletTable_.walletId, it.wallet.id)
                        .build()
                        .findFirst()
                it.walletName = result?.walletName ?: ""
                it.walletDefault = result?.walletDefault ?: 0
            }
            return@zip myWalletList
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
    fun createEthWallet(walletType: WalletType, mnemonicList: MutableList<String>, walletName: String, walletPassword: String): Observable<MyWallet> {

        return Observable.just(mnemonicList)
                .flatMap {
                    if (it.size == 12) {
                        return@flatMap Observable.just(it.joinToString(" "))
                    } else {
                        return@flatMap Observable.error(WalletException(WalletExceptionCode.ERROR_MNEMONIC))
                    }
                }
                .flatMap { importWalletByMnemonic(walletType, it, walletName, walletPassword) }
                .flatMap { myWallet ->
                    myWallet.walletDefault = 1
                    return@flatMap Observable.just(myWallet)
                }.changeIOThread()

    }

    /**
     * 获取当前钱包的助记词
     * @param walletId 钱包Id
     * @param  walletPassword 钱包密码
     */
    fun getWalletMnemonic(walletId: String, walletPassword: String): Observable<MutableList<String>> {
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
     * 根据类型获取钱包列表
     * @param walletType 钱包类型
     */
    fun getWalletListByType(walletType: WalletType): Observable<MutableList<MyWallet>> {
        return getIdentity(walletType).flatMap {
            try {
                val myWalletList = mutableListOf<MyWallet>()
                it.getWallets(walletType).forEach { wallet ->
                    val data = ObjectBox.getMyWalletTableManager()
                            .query()
                            .equal(MyWalletTable_.walletId, wallet.id)
                            .build()
                            .findFirst()
                    val walletName = data?.walletName ?: ""
                    val walletDefault = data?.walletDefault ?: 0
                    myWalletList.add(MyWallet(wallet, walletType, walletName, walletDefault))
                }
                return@flatMap Observable.just(myWalletList)
            } catch (e: TokenException) {
                return@flatMap Observable.error(e)
            }

        }
    }

    /**
     * 根据助记词导入钱包
     * @param walletType 钱包类型
     * @param mnemonic 助记词
     * @param walletName 钱包名称
     * @param walletPassword 钱包密码
     */
    fun importWalletByMnemonic(walletType: WalletType, mnemonic: String, walletName: String, walletPassword: String): Observable<MyWallet> {
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
                            val myWallet = MyWallet(wallet, walletType, walletName, 1)
                            val myWalletTable = MyWalletTable(myWallet.wallet.id, walletName)
                            myWalletTable.walletDefault = 1
                            ObjectBox.getMyWalletTableManager()
                                    .put(myWalletTable)
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
    fun importWalletByPrivateKey(walletType: WalletType, privateKey: String, walletName: String, walletPassword: String): Observable<MyWallet> {
        return getIdentity(walletType)
                .flatMap {
                    try {
                        val metadata = Metadata()
                        metadata.source = Metadata.FROM_MNEMONIC
                        metadata.network = WalletConfig.ETH_NET_WORK
                        metadata.segWit = Metadata.P2WPKH
                        metadata.chainType = ChainType.ETHEREUM
                        val wallet = WalletManager.importWalletFromPrivateKey(walletType, metadata, privateKey, walletPassword, true)
                        val myWalletTable = MyWalletTable(wallet.id, walletName)
                        myWalletTable.walletDefault = 1
                        ObjectBox.getMyWalletTableManager()
                                .put(myWalletTable)

                        return@flatMap Observable.just(MyWallet(wallet, walletType, walletName, 1))
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
                    val count = ObjectBox.getMyWalletTableManager()
                            .query()
                            .equal(MyWalletTable_.walletId, walletId)
                            .build()
                            .remove()
                    return@flatMap Observable.just(count > 0)
                }

    }

    /**
     * 设置默认钱包
     * @param walletId 钱包Id
     */
    fun setDefaultWalletById(walletId: String): Observable<Boolean> {
        return Observable.create {
            val oldDefault = ObjectBox.getMyWalletTableManager()
                    .query()
                    .equal(MyWalletTable_.walletDefault, 1)
                    .build()
                    .findFirst()
            if (oldDefault != null) {
                oldDefault.walletDefault = 0
                ObjectBox.getMyWalletTableManager()
                        .put(oldDefault)
            }
            val data = ObjectBox.getMyWalletTableManager()
                    .query()
                    .equal(MyWalletTable_.walletId, walletId)
                    .build()
                    .findFirst()
            if (data != null) {
                data.walletDefault = 1
                val count = ObjectBox.getMyWalletTableManager()
                        .put(data)
                it.onNext(count > 0)
            } else {
                it.onNext(false)
            }
        }

    }

    /**
     * 添加默认钱包
     */
    fun setDefaultWalletByWallet(walletTable: MyWalletTable): Observable<Boolean> {
        return Observable.create<Boolean> {
            val oldDefault = ObjectBox.getMyWalletTableManager()
                    .query()
                    .equal(MyWalletTable_.walletDefault, 1)
                    .build()
                    .findFirst()
            if (oldDefault != null) {
                oldDefault.walletDefault = 0
                ObjectBox.getMyWalletTableManager()
                        .put(oldDefault)
            }
            walletTable.walletDefault = 1
            val count = ObjectBox.getMyWalletTableManager()
                    .put(walletTable)
            it.onNext(count > 0)
        }.changeIOThread()
    }

}