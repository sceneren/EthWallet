package wiki.scene.eth.wallet.core.util

import io.reactivex.Observable
import org.consenlabs.tokencore.wallet.Identity
import org.consenlabs.tokencore.wallet.Wallet
import org.consenlabs.tokencore.wallet.WalletManager
import org.consenlabs.tokencore.wallet.model.ChainType
import org.consenlabs.tokencore.wallet.model.Metadata
import wiki.scene.eth.wallet.core.config.WalletConfig
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.exception.WalletException
import wiki.scene.eth.wallet.core.exception.WalletExceptionCode
import wiki.scene.eth.wallet.core.ext.changeNewThread

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
                //删除默认生成的钱包
                identity.wallets.forEach { wallet ->
                    WalletManager.removeWallet(wallet.id, "")
                }
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
     * 获取所以的钱包列表
     */
    fun getWalletList(): Observable<MutableList<Wallet>> {
        return Observable.zip(getIdentity(WalletType.ETH_WALLET_TYPE_SET), getIdentity(WalletType.ETH_WALLET_TYPE_ETH), { setIdentity, ethIdentity ->
            val setWalletList = setIdentity.wallets.toMutableList()
            val ethWalletList = ethIdentity.wallets.toMutableList()
            setWalletList.addAll(ethWalletList)
            return@zip setWalletList
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
    fun createEthWallet(walletType: WalletType, walletName: String, walletPassword: String): Observable<Wallet> {
        return getIdentity(walletType).flatMap {
            val wallet = it.deriveWallets(arrayListOf(ChainType.ETHEREUM), walletPassword)[0]
            wallet.setAccountName(walletName)
            return@flatMap Observable.just(wallet)
        }.changeNewThread()

    }

    /**
     * 获取当前钱包的助记词
     * @param walletId 钱包Id
     * @param  walletPassword 钱包密码
     */
    fun getCurrentWalletMnemonic(walletId: String, walletPassword: String): Observable<MutableList<String>> {
        return Observable.create<MutableList<String>> {
            val mnemonic = WalletManager.exportMnemonic(walletId, walletPassword)
            val mnemonicList = mnemonic.mnemonic.split(" ").toMutableList()
            it.onNext(mnemonicList)
        }.changeNewThread()
    }


    /**
     * 获取钱包列表
     * @param walletType 钱包类型
     */
    fun getWalletListByType(walletType: WalletType): Observable<MutableList<Wallet>> {
        return getIdentity(walletType).flatMap {
            return@flatMap Observable.just(it.wallets)
        }
    }

    /**
     * 根据助记词导入钱包
     * @param walletType 钱包类型
     * @param mnemonic 助记词
     * @param walletName 钱包名称
     * @param walletPassword 钱包密码
     */
    fun importWalletByMnemonic(walletType: WalletType, mnemonic: String, walletName: String, walletPassword: String): Observable<Wallet> {
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
                        val wallet = WalletManager.importWalletFromMnemonic(metadata, mnemonic, "m/44'/60'/0'/0/0", walletPassword, true)
                        wallet.setAccountName(walletName)
                        return@flatMap Observable.just(wallet)
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
    fun importWalletByPrivateKey(walletType: WalletType, privateKey: String, walletName: String, walletPassword: String): Observable<Wallet> {
        return getIdentity(walletType)
                .flatMap {
                    val metadata = Metadata()
                    metadata.source = Metadata.FROM_MNEMONIC
                    metadata.network = WalletConfig.ETH_NET_WORK
                    metadata.segWit = Metadata.P2WPKH
                    val wallet = WalletManager.importWalletFromPrivateKey(metadata, privateKey, walletPassword, true)
                    wallet.setAccountName(walletName)
                    return@flatMap Observable.just(wallet)
                }.changeNewThread()
    }

}