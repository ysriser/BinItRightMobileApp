package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RedeemResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Response
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RewardShopFragmentTest {

    @Test
    fun onViewCreated_loadShopItems_showsBalanceAndList() {
        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.success(
                    listOf(Accessory(1L, "hat", "", 50, owned = false, equipped = false))
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = RewardShopFragment().apply {
            arguments = Bundle().apply { putInt("totalPoints", 120) }
        }

        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        waitMainUntil {
            fragment.requireView().findViewById<TextView>(R.id.tvBalance).text.toString()
                .contains("120")
        }

        val root = fragment.requireView()
        assertEquals("Your Balance: 120 points", root.findViewById<TextView>(R.id.tvBalance).text.toString())
        assertEquals(1, fragment.requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvShop).adapter?.itemCount)

        root.findViewById<View>(R.id.btnBack).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(fragment.isAdded || fragment.isDetached)
    }

    @Test
    fun redeem_andEquip_coverSuccessFailureAndExceptionBranches() {
        val item = Accessory(8L, "cap", "", 30, owned = false, equipped = false)
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.success(listOf(item))
                "redeemRewardShopItem" -> Response.success(RedeemResponse(88, "Redeemed"))
                "equipAccessory" -> Response.success<Void>(null)
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val fragment = RewardShopFragment().apply {
            arguments = Bundle().apply { putInt("totalPoints", 100) }
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        callPrivate(fragment, "redeem", item)
        waitMainUntil {
            fragment.requireView().findViewById<TextView>(R.id.tvBalance).text.toString().contains("88")
        }
        assertEquals(
            "Your Balance: 88 points",
            fragment.requireView().findViewById<TextView>(R.id.tvBalance).text.toString()
        )

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.success(listOf(item))
                "redeemRewardShopItem" -> Response.error<RedeemResponse>(
                    400,
                    "bad request".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "redeem", item)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(
            "Your Balance: 88 points",
            fragment.requireView().findViewById<TextView>(R.id.tvBalance).text.toString()
        )

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.success(listOf(item))
                "redeemRewardShopItem" -> throw IllegalStateException("redeem boom")
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "redeem", item)
        shadowOf(Looper.getMainLooper()).idle()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.success(listOf(item))
                "equipAccessory" -> Response.success<Void>(null)
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "equip", item)
        shadowOf(Looper.getMainLooper()).idle()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.success(listOf(item))
                "equipAccessory" -> Response.error<Void>(
                    500,
                    "equip failed".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "equip", item)
        shadowOf(Looper.getMainLooper()).idle()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.success(listOf(item))
                "equipAccessory" -> throw IllegalStateException("equip boom")
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "equip", item)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun loadShopItems_failureAndException_keepScreenStable() {
        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> Response.error<List<Accessory>>(
                    500,
                    "server error".toResponseBody("text/plain".toMediaType())
                )
                else -> throw UnsupportedOperationException(methodName)
            }
        }

        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = RewardShopFragment().apply {
            arguments = Bundle().apply { putInt("totalPoints", 66) }
        }
        activity.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        shadowOf(Looper.getMainLooper()).idle()

        installApiServiceStub { methodName, _ ->
            when (methodName) {
                "getRewardShopItems" -> throw IllegalStateException("network down")
                else -> throw UnsupportedOperationException(methodName)
            }
        }
        callPrivate(fragment, "loadShopItems")
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            "Your Balance: 66 points",
            fragment.requireView().findViewById<TextView>(R.id.tvBalance).text.toString()
        )
    }

    private fun callPrivate(target: Any, methodName: String, vararg args: Any?) {
        val method = target.javaClass.declaredMethods.first {
            it.name == methodName && it.parameterTypes.size == args.size
        }
        method.isAccessible = true
        method.invoke(target, *args)
    }

    private fun installApiServiceStub(handler: (methodName: String, args: Array<Any?>?) -> Any?) {
        val proxy = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "toString" -> "ApiServiceStub"
                "hashCode" -> 0
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ApiService

        val apiField = RetrofitClient::class.java.getDeclaredField("api")
        apiField.isAccessible = true
        apiField.set(RetrofitClient, proxy)
    }

    private fun waitMainUntil(loops: Int = 20, condition: () -> Boolean) {
        repeat(loops) {
            if (condition()) return
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
