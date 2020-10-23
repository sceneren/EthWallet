package wiki.scene.eth.wallet.core.util

import io.reactivex.Observable
import org.consenlabs.tokencore.wallet.Identity
import org.consenlabs.tokencore.wallet.Wallet
import org.consenlabs.tokencore.wallet.WalletManager
import org.consenlabs.tokencore.wallet.model.ChainType
import org.consenlabs.tokencore.wallet.model.Metadata
import org.consenlabs.tokencore.wallet.model.Network
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

    private const val ETH_NET_WORK = Network.MAINNET

    private fun getIdentity(): Observable<Identity> {
        return Observable.create<Identity> {
            if (Identity.getCurrentIdentity() == null) {
                val identity = Identity.createIdentity("Identity", "", "", ETH_NET_WORK, Metadata.P2WPKH)
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
     * 创建助记词
     */
    fun createMnemonic(): Observable<MutableList<String>> {
        return Observable.create<MutableList<String>> {
            it.onNext(MnemonicUtil.randomMnemonicCodes())
        }.changeNewThread()
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
    fun createEthWallet(walletName: String, walletPassword: String): Observable<Wallet> {
        return getIdentity().flatMap<Wallet> {
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

}