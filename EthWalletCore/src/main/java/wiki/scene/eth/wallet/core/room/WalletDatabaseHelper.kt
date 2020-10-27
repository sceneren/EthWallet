package wiki.scene.eth.wallet.core.room

import androidx.room.Room
import wiki.scene.eth.wallet.core.util.EthWalletCore

object WalletDatabaseHelper {
    private var appDatabase: WalletDataBase

    init {
        //MyApp 是自定义application类
        val appContext = EthWalletCore.getContext()
        appDatabase = Room.databaseBuilder(appContext, WalletDataBase::class.java, "wallet-db")
                .allowMainThreadQueries()
                .build()
    }

    fun getInstance(): WalletDataBase {
        return appDatabase
    }
}