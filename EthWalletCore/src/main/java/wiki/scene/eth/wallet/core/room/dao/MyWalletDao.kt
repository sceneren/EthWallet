package wiki.scene.eth.wallet.core.room.dao

import androidx.room.*
import wiki.scene.eth.wallet.core.room.table.MyWalletTable

@Dao
interface MyWalletDao {
    @Query("SELECT * FROM MyWallet WHERE 'walletId' = :walletId")
    fun queryWalletById(walletId: String): MyWalletTable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveWallet(myWalletTable: MyWalletTable)

    @Query("DELETE FROM MyWallet WHERE 'walletId' = :walletId")
    fun deleteWalletById(walletId: String)
}