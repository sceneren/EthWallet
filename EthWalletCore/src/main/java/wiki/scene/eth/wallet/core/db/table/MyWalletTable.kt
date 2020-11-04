package wiki.scene.eth.wallet.core.db.table

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import wiki.scene.eth.wallet.core.config.WalletType

@Entity
class MyWalletTable(
        @Index
        var walletId: String = "",
        var walletName: String = "",
        var walletTypeInt: Int = WalletType.ETH_WALLET_TYPE_ETH.ordinal
) {
    @Id
    var id: Long = 0L
    var walletDefault: Int = 0//0-不是默认 1-默认
    var walletListImageRes: Int = 0
    var createTime: Long = 0

    val walletType: WalletType
        get() = WalletType.values()[walletTypeInt]
}