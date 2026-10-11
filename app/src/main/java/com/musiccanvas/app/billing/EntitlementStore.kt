package com.musiccanvas.app.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * R1 预留的权益存储抽象（PRD 15.2：与歌曲信息完全隔离）。
 *
 * B1 全部功能免费测试，不做真实购买。
 * R1 接入渠道（Google Play Billing / 国内商店支付）时实现 [EntitlementStore]
 * 并满足 MC-060..MC-064：pending 不解锁、验单后授权益、重复回调幂等、
 * 可主动恢复、离线可用已验证权益、撤销按规则更新。
 */
interface EntitlementStore {
    val unlockedThemes: StateFlow<Set<String>>

    suspend fun has(themeId: String): Boolean = unlockedThemes.value.contains(themeId)

    /** R1：发起购买。B1 抛出未实现。 */
    suspend fun purchase(themeId: String): PurchaseResult = PurchaseResult.NotAvailable

    /** R1：主动恢复购买。 */
    suspend fun restore(): Boolean = false
}

enum class PurchaseResult { Granted, Pending, Cancelled, Failed, NotAvailable }

/** B1 封闭测试：全部主题免费体验（PRD 路线图 B1）；R1 替换为真实渠道实现。 */
class FreeBetaEntitlementStore : EntitlementStore {
    override val unlockedThemes: StateFlow<Set<String>> =
        MutableStateFlow(setOf("warm_vinyl", "blank_cover"))
}
