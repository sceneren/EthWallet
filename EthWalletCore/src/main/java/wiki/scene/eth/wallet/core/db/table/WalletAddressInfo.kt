package wiki.scene.eth.wallet.core.db.table

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import wiki.scene.eth.wallet.core.config.WalletType

@Entity
data class WalletAddressInfo(
        var logo: Int = 0,
        var walletTypeInt: Int = WalletType.ETH_WALLET_TYPE_ETH.ordinal,
        var walletAddress: String = "",
        var remark: String = "",
) {
    @Id
    var addressId: Long = 0
    var createTime: Long = 0

    val walletType: WalletType
        get() = WalletType.values()[walletTypeInt]
}