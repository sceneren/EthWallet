package wiki.scene.eth.wallet.core.db.manager

import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo_

object OtherWalletDBManager {
    fun insert(otherWalletInfo: OtherWalletInfo) {
        var sy = otherWalletInfo.sy
        if (sy.startsWith("0x")) {
            sy = sy.substring(2)
        }
        otherWalletInfo.sy = sy
        ObjectBox.getOtherWalletInfoManager()
                .put(otherWalletInfo)
    }

    fun queryWalletByPrivateKey(privateKey: String): OtherWalletInfo? {
        var sy = privateKey
        if (sy.startsWith("0x")) {
            sy = sy.substring(2)
        }
        return ObjectBox.getOtherWalletInfoManager()
                .query()
                .equal(OtherWalletInfo_.sy, sy)
                .build()
                .findFirst()
    }

    fun queryWalletByMnemonic(walletMnemonic: String): OtherWalletInfo? {
        return ObjectBox.getOtherWalletInfoManager()
                .query()
                .equal(OtherWalletInfo_.mn, walletMnemonic)
                .build()
                .findFirst()
    }
}