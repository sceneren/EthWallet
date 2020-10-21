package wiki.scene.eth.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.web3j.crypto.WalletFile
import wiki.scene.eth.wallet.core.util.EthWalletUtils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createWallet.setOnClickListener {
            createWallet()
        }
        createMnemonic.setOnClickListener {
            createMnemonic()
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun createWallet() {
//        EthWalletUtils.createLightWallet("111111")
//                .subscribe({
//                    tvResult1.text = it.address+"\n"+it.address.length
//                    Log.e("xxx", it.toString())
//                }, {
//                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
//                })
    }

    @SuppressLint("CheckResult")
    private fun createMnemonic() {
        EthWalletUtils.createMnemonic()
                .subscribe({
                    tvResult2.text = it
                    Log.e("xxx", it.toString())
                }, {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                })
    }
}