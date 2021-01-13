package wiki.scene.eth.wallet

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.orhanobut.hawk.Hawk
import com.tencent.mmkv.MMKV
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_main.*
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.manager.WalletAddressDBManager
import wiki.scene.eth.wallet.core.db.table.WalletAddressInfo
import wiki.scene.eth.wallet.core.ext.changeIOThread
import wiki.scene.eth.wallet.core.util.WalletSignUtil
import wiki.scene.eth.wallet.core.util.WalletUtils

class MainActivity : AppCompatActivity() {
    private val mmkv by lazy {
        MMKV.defaultMMKV()
    }

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
//            val string = "{\"memo\":\"1\",\"to\":\"SET62sWobH6CyWMRjdyJRzhUspxPpNEx1P5M\",\"ctime\":\"1610503406402\",\"value\":\"1.0\",\"from\":\"SET5PrmA8nj32ioEUcw3DGgU8v5RtQUTSAGK\",\"assetsId\":\"1\",\"nonce\":\"1\"}"
//            val sign = WalletSignUtil.sign(string, "4f2781b83269d87c23229f8bf7822a45cd055b40bc8bbb066876b45b0667299c")

//            WalletUtils.hasWallet()
//                    .subscribe({
//                        showLog("是否有钱包：$it")
//                    }, {
//                        showLog(it.message!!)
//                    })

//            val result = WalletSignUtil.sign("陈老板测试", "0xbf534b8857e824171f04528aeddea0990a5748b076e839b97ba91fb1010d7ef6")
//            LogUtils.e(result)

//            Observable.create<Long> {
//                LogUtils.e("开始执行")
//                val hawkTime = System.currentTimeMillis()
//                for (i in 0..10000) {
//                    Hawk.put("xx${i}", "xxxx$i")
//                }
//                it.onNext(System.currentTimeMillis() - hawkTime)
//                it.onComplete()
//            }.changeIOThread()
//                    .subscribe {
//                        LogUtils.e("Hawk耗时：${it}")
//                    }
//            Observable.create<Long> {
//                val mmkvTime = System.currentTimeMillis()
//                for (i in 0..10000) {
//                    mmkv?.encode("xx${i}", "xxxx$i")
//                }
//                it.onNext(System.currentTimeMillis() - mmkvTime)
//                it.onComplete()
//            }.changeIOThread()
//                    .subscribe {
//                        LogUtils.e("MMKV耗时：${it}")
//                    }
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