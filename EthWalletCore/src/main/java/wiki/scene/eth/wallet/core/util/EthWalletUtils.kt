package wiki.scene.eth.wallet.core.util

import io.reactivex.Observable
import org.consenlabs.tokencore.foundation.utils.MnemonicUtil
import org.consenlabs.tokencore.wallet.model.ChainType
import org.consenlabs.tokencore.wallet.model.Metadata
import wiki.scene.eth.wallet.core.bean.MyWallet
import wiki.scene.eth.wallet.core.config.WalletConfig
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.exception.WalletException
import wiki.scene.eth.wallet.core.exception.WalletExceptionCode
import wiki.scene.eth.wallet.core.ext.changeNewThread
import wiki.scene.eth.wallet.core.room.WalletDatabaseHelper
import wiki.scene.eth.wallet.core.room.table.MyWalletTable

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
            if (Identity.getCurrentIdentity() == null) {
                val identity = Identity.createIdentity(walletType.name, "", "", WalletConfig.ETH_NET_WORK, Metadata.P2WPKH)
                it.onNext(identity)
            } else {
                it.onNext(Identity.currentIdentity)
            }
        }.changeNewThread()


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
            val setWalletList = setIdentity.wallets.toMutableList()
            val ethWalletList = ethIdentity.wallets.toMutableList()
            setWalletList.forEach {
                myWalletList.add(MyWallet(it, WalletType.ETH_WALLET_TYPE_SET, ""))
            }
            ethWalletList.forEach {
                myWalletList.add(MyWallet(it, WalletType.ETH_WALLET_TYPE_ETH, ""))
            }

            myWalletList.forEach {
                val walletTable = WalletDatabaseHelper.getInstance()
                        .myWalletDao()
                        .queryWalletById(it.wallet.id)
                val walletName = walletTable?.walletName ?: ""
                it.walletName = walletName
            }


            return@zip myWalletList
        }).changeNewThread()
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
        }.changeNewThread()
    }

    /**
     * 创建钱包
     * @param walletName 钱包名称
     * @param walletPassword 钱包密码
     */
    fun createEthWallet(walletType: WalletType, walletName: String, walletPassword: String): Observable<MyWallet> {

        return Observable.just(MnemonicUtil.randomMnemonicCodes())
                .flatMap { Observable.just(it.joinToString(" ")) }
                .flatMap { importWalletByMnemonic(walletType, it, walletName, walletPassword) }
                .zipWith(getIdentity(walletType), { myWallet, identity ->
                    identity.addWallet(myWallet.wallet)
                    WalletDatabaseHelper.getInstance()
                            .myWalletDao()
                            .saveWallet(MyWalletTable(myWallet.wallet.id, walletName))
                    return@zipWith myWallet
                }).changeNewThread()

    }

    /**
     * 获取当前钱包的助记词
     * @param walletId 钱包Id
     * @param  walletPassword 钱包密码
     */
    fun getWalletMnemonic(walletId: String, walletPassword: String): Observable<MutableList<String>> {
        return Observable.create<MutableList<String>> {
            val mnemonic = WalletManager.exportMnemonic(walletId, walletPassword)
            val mnemonicList = mnemonic.mnemonic.split(" ").toMutableList()
            it.onNext(mnemonicList)
        }.changeNewThread()
    }


    /**
     * 根据类型获取钱包列表
     * @param walletType 钱包类型
     */
    fun getWalletListByType(walletType: WalletType): Observable<MutableList<MyWallet>> {
        return getIdentity(walletType).flatMap {
            val myWalletList = mutableListOf<MyWallet>()
            it.wallets.forEach { wallet ->
                val walletTable = WalletDatabaseHelper.getInstance()
                        .myWalletDao()
                        .queryWalletById(wallet.id)

                val walletName = walletTable?.walletName ?: ""
                myWalletList.add(MyWallet(wallet, walletType, walletName))
            }
            return@flatMap Observable.just(myWalletList)
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
                    val mnemonicList = mnemonic.split(" ").toMutableList()
                    if (mnemonicList.size != 12) {
                        throw WalletException(WalletExceptionCode.ERROR_MNEMONIC)
                    } else {
                        val metadata = Metadata()
                        metadata.source = Metadata.FROM_MNEMONIC
                        metadata.network = WalletConfig.ETH_NET_WORK
                        metadata.segWit = Metadata.P2WPKH
                        metadata.chainType = ChainType.ETHEREUM
                        val wallet = WalletManager.importWalletFromMnemonic(metadata, mnemonic, "m/44'/60'/0'/0/0", walletPassword, true)
                        val walletTable = MyWalletTable(wallet.id, walletName)
                        WalletDatabaseHelper.getInstance()
                                .myWalletDao()
                                .saveWallet(walletTable)
                        return@flatMap Observable.just(MyWallet(wallet, walletType, walletName))
                    }
                }.changeNewThread()
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
                    val metadata = Metadata()
                    metadata.source = Metadata.FROM_MNEMONIC
                    metadata.network = WalletConfig.ETH_NET_WORK
                    metadata.segWit = Metadata.P2WPKH
                    val wallet = WalletManager.importWalletFromPrivateKey(metadata, privateKey, walletPassword, true)
                    val walletTable = MyWalletTable(wallet.id, walletName)
                    WalletDatabaseHelper.getInstance()
                            .myWalletDao()
                            .saveWallet(walletTable)
                    return@flatMap Observable.just(MyWallet(wallet, walletType, walletName))
                }.changeNewThread()
    }

    /**
     * 删除钱包根据Id
     */
    fun deleteWalletById(walletId: String, password: String): Observable<Boolean> {
        return Observable.create<Boolean> {
            try {
                WalletManager.removeWallet(walletId, password)
                WalletDatabaseHelper.getInstance()
                        .myWalletDao()
                        .deleteWalletById(walletId)
                it.onNext(true)
            } catch (e: Exception) {
                it.onError(WalletException(WalletExceptionCode.ERROR_PASSWORD))
            }
        }.changeNewThread()
    }

}