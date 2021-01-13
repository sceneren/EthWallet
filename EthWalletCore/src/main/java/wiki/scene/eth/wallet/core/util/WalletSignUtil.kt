package wiki.scene.eth.wallet.core.util

import android.util.Log
import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import java.math.BigInteger

object WalletSignUtil {
    fun sign(data: String, privateKey: String): String {
        try {
            val realPrivateKey = if (privateKey.startsWith("0x")) {
                privateKey.substring(2)
            } else {
                privateKey
            }
            Log.d("====>realPrivateKey", realPrivateKey)
            val realPrivateKeyInt = BigInteger(realPrivateKey, 16)
            Log.d("====>realPrivateKeyInt", realPrivateKeyInt.toString())
            val pubKey = Sign.publicKeyFromPrivate(realPrivateKeyInt)
            Log.d("====>pubKey", pubKey.toString())
            val keyPair = ECKeyPair(realPrivateKeyInt, pubKey)
            Log.d("====>pubKey", keyPair.toString())
            val msgHash = Hash.sha3(data.toByteArray())
            Log.d("====>msgHash", msgHash.toString())
            val signature = Sign.signMessage(msgHash, keyPair, false)
            Log.d("====>signature.v", Hex.toHexString(signature.v))
            Log.d("====>signature.r", Hex.toHexString(signature.r))
            Log.d("====>signature.s", Hex.toHexString(signature.s))
            val result = Hex.toHexString(signature.v) + Hex.toHexString(signature.r) + Hex.toHexString(signature.s) + pubKey.toString(16)
            Log.d("====>result", result)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

    }
}