package wiki.scene.eth.wallet.core.db.table

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class OtherWalletInfo(
        var address: String = "",
        var sy: String = ""
) {
    @Id
    var id: Long = 0
}