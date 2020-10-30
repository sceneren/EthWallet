package wiki.scene.eth.wallet.core.db.table

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class MyWalletTable(
        @Index
        val walletId: String = "",
        val walletName: String = ""
){
        @Id
        var id: Long = 0L
}