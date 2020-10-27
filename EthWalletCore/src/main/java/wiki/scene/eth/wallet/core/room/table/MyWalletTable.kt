package wiki.scene.eth.wallet.core.room.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MyWallet")
data class MyWalletTable(
        val walletId: String,
        val walletName: String
) {
    @PrimaryKey
    var id = 0
}