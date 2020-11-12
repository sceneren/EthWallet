package wiki.scene.eth.wallet

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.GsonUtils
import kotlinx.android.synthetic.main.activity_main.*
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.manager.WalletAddressDBManager
import wiki.scene.eth.wallet.core.db.table.WalletAddressInfo
import wiki.scene.eth.wallet.core.util.WalletUtils

class MainActivity : AppCompatActivity() {

    private fun showLog(msg: String) {
        Log.e("日志", msg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvResult1.text = System.currentTimeMillis().toString()

        verificationCode.setBgColor(Color.RED)
        verificationCode.verificationCode = "asdf"

        btnCheckHasWallet.setOnClickListener {
            WalletUtils.hasWallet()
                    .subscribe({
                        showLog("是否有钱包：$it")
                    }, {
                        showLog(it.message!!)
                    })
        }

        getCurrentWallet.setOnClickListener {
            WalletUtils.getDefaultWallet()
                    .subscribe({
                        showLog("是否有钱包：$it")
                    }, {
                        showLog(it.message!!)
                    })
        }

        createWallet.setOnClickListener {
            WalletUtils.createMnemonic()
                    .flatMap {
                        showLog(it.toString())
                        return@flatMap WalletUtils.createWallet(WalletType.ETH_WALLET_TYPE_ETH, "钱包${Math.random()}", "12345678", it.joinToString(" "), 0)
                    }.subscribe({
                        showLog(GsonUtils.toJson(it))
                    }, {
                        showLog(it.message!!)
                    })


        }
        createMnemonic.setOnClickListener {
            WalletUtils.getWalletList()
                    .subscribe({
                        showLog(GsonUtils.toJson(it))
                    }, {
                        showLog(it.message!!)
                    })
        }

        btnImportWalletByPrivateKey.setOnClickListener {

        }


        btnInsetAddress.setOnClickListener {
            WalletAddressDBManager.addOrUpdateWalletAddress(WalletAddressInfo(0, WalletType.ETH_WALLET_TYPE_ETH.ordinal, "0x1231213", "备注"))
                    .subscribe {
                        Log.e("xx", it.toString())
                    }
        }
        btnDeleteAddress.setOnClickListener {
            WalletAddressDBManager.deleteWalletAddressById(1L)
                    .subscribe({
                        Log.e("xx", it.toString())
                    }, {
                        Log.e("xx", it.message!!)
                    })
        }

        btnQueryAddress.setOnClickListener {
            WalletAddressDBManager.queryWalletAddress()
                    .subscribe({
                        Log.e("xx", it.toString())
                    }, {
                        Log.e("xx", it.message!!)
                    })
        }

        btnUpdateAddress.setOnClickListener {
            val addressInfo = WalletAddressInfo(0, 0, "xxx", "xxxxxxx")
            addressInfo.addressId = 1
            WalletAddressDBManager.addOrUpdateWalletAddress(addressInfo)
        }


    }

}