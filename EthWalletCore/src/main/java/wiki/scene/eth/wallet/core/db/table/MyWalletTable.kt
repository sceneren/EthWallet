package wiki.scene.eth.wallet.core.db.table

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
class MyWalletTable(
        @Index
        val walletId: String = "",
        val walletName: String = ""
) {
    @Id
    var id: Long = 0L
    var walletDefault: Int = 0//0-不是默认 1-默认
    var walletListImageRes: Int = 0
    var createTime: Long = 0
}