package wiki.scene.eth.wallet.core.manager

import io.objectbox.kotlin.query
import io.reactivex.Observable
import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.MyWalletTable
import wiki.scene.eth.wallet.core.db.table.MyWalletTable_
import wiki.scene.eth.wallet.core.ext.changeIOThread

/**
 *
 * @Description:    钱包数据库管理
 * @Author:         scene
 * @CreateDate:     2020/11/2 10:47
 * @UpdateUser:     更新者：
 * @UpdateDate:     2020/11/2 10:47
 * @UpdateRemark:   更新说明：
 * @Version:        1.0.0
 */
object MyWalletTableManager {

    fun queryWallet(): MutableList<MyWalletTable> {
        return ObjectBox.getMyWalletTableManager()
                .query()
                .build()
                .find()
    }

    fun queryWalletByWalletId(walletId: String): MyWalletTable? {
        return ObjectBox.getMyWalletTableManager()
                .query()
                .equal(MyWalletTable_.walletId, walletId)
                .build()
                .findFirst()
    }

    fun insertOrUpdateWallet(myWalletTable: MyWalletTable, walletDefault: Int, walletListImageRes: Int): Boolean {
        myWalletTable.walletDefault = walletDefault
        myWalletTable.walletListImageRes = walletListImageRes
        if (myWalletTable.id == 0L) {
            myWalletTable.createTime = System.currentTimeMillis()
        }
        val count = ObjectBox.getMyWalletTableManager()
                .put(myWalletTable)
        return count > 0
    }

    fun insertOrUpdateWallet(myWalletTable: MyWalletTable): Boolean {
        return insertOrUpdateWallet(myWalletTable, myWalletTable.walletDefault, myWalletTable.walletListImageRes)
    }

    fun deleteWalletByWalletId(walletId: String): Observable<Boolean> {
        return Observable.create<Boolean> {
            val count = ObjectBox.getMyWalletTableManager()
                    .query()
                    .equal(MyWalletTable_.walletId, walletId)
                    .build()
                    .remove()
            it.onNext(count > 0)
        }.changeIOThread()
    }

    fun queryDefaultWallet(): MyWalletTable? {
        return ObjectBox.getMyWalletTableManager()
                .query()
                .equal(MyWalletTable_.walletDefault, 1)
                .build()
                .findFirst()
    }

    fun setDefaultWalletByWalletId(walletId: String): Boolean {
        val chooseWallet = ObjectBox.getMyWalletTableManager()
                .query()
                .equal(MyWalletTable_.walletId, walletId)
                .build()
                .findFirst()
        return if (chooseWallet != null) {
            chooseWallet.walletDefault = 1
            ObjectBox.getMyWalletTableManager()
                    .put(chooseWallet) > 0
        } else {
            false
        }

    }
}