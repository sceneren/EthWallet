package wiki.scene.eth.wallet.core.bean

import org.consenlabs.tokencore.wallet.Wallet
import org.consenlabs.tokencore.wallet.keystore.IMTKeystore
import wiki.scene.eth.wallet.core.config.WalletType

/**
 *
 * @Description:    自定义的钱包
 * @Author:         scene
 * @CreateDate:     2020/10/26 11:51
 * @UpdateUser:     更新者：
 * @UpdateDate:     2020/10/26 11:51
 * @UpdateRemark:   更新说明：
 * @Version:        1.0.0
 */
data class MyWallet(
        val wallet: Wallet,
        val walletType: WalletType
)