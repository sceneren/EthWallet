package wiki.scene.eth.wallet.core.db.table

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import wiki.scene.eth.wallet.core.config.WalletType

@Entity
class WalletInfoTable(
        var walletName: String = "",
        var walletTypeInt: Int = WalletType.ETH_WALLET_TYPE_ETH.ordinal,
        var walletMnemonic: String = "",
        var walletPrivateKey: String = "",
        var walletPublicKey: String = "",
        var walletAddress: String = "",
        var walletPassword: String = ""
) {
    @Id
    var id: Long = 0L
    var walletDefault: Int = 0//0-不是默认 1-默认
    var walletListImageRes: Int = 0
    var createTime: Long = 0
}