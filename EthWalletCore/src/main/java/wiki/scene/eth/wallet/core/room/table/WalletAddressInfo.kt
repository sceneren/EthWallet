package wiki.scene.eth.wallet.core.room.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_address")
data class WalletAddressInfo(
        @PrimaryKey(autoGenerate = true)
        var addressId: Int,
        val logo: Int,
        val walletType: Int,
        val walletAddress: String,
        val remark: String
) {
    @PrimaryKey
    var id = 0
}