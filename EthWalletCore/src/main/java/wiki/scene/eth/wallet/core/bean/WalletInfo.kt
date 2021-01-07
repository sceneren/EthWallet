package wiki.scene.eth.wallet.core.bean

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import wiki.scene.eth.wallet.core.config.WalletType

@Parcelize
class WalletInfo(
        var id: Long,
        var walletName: String = "",
        var walletAddress: String = "",
        var walletListImageRes: Int = 0,
        val walletType: WalletType,
        var walletDefault: Boolean,//0-不是默认 1-默认
) : Parcelable