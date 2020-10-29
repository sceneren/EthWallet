package wiki.scene.eth.wallet.core.room.dao

import androidx.room.*
import wiki.scene.eth.wallet.core.room.table.WalletAddressInfo

@Dao
interface WalletAddressDao {
    @Query("SELECT * FROM wallet_address WHERE 'addressId'=:addressId LIMIT 1")
    fun queryWalletAddressById(addressId: Int): MutableList<WalletAddressInfo>

    @Query("SELECT * FROM wallet_address")
    fun queryAllWalletAddress(): MutableList<WalletAddressInfo>

    @Query("DELETE FROM wallet_address WHERE 'addressId'=:addressId")
    fun deleteWalletAddressById(addressId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addOrUpdateWalletAddress(walletAddressInfo: WalletAddressInfo)

}