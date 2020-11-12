package wiki.scene.eth.wallet.core.db.manager

import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo

object MyOtherWalletDBManager {
    fun insert(otherWalletInfo: OtherWalletInfo) {
        ObjectBox.getOtherWalletInfoManager()
                .put(otherWalletInfo)
    }
}