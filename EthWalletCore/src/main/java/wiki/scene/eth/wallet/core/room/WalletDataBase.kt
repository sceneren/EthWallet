package wiki.scene.eth.wallet.core.room

import androidx.room.Database
import androidx.room.RoomDatabase
import wiki.scene.eth.wallet.core.room.dao.MyWalletDao
import wiki.scene.eth.wallet.core.room.dao.WalletAddressDao
import wiki.scene.eth.wallet.core.room.table.MyWalletTable

@Database(entities = [MyWalletTable::class], version = 1, exportSchema = true)
abstract class WalletDataBase : RoomDatabase() {

    abstract fun myWalletDao(): MyWalletDao
    abstract fun walletAddressDao(): WalletAddressDao
}