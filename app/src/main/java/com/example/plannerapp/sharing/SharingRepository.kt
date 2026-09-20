package com.example.plannerapp.sharing

import android.net.Uri
import com.example.plannerapp.credits.CreditRepository
import kotlinx.coroutines.flow.Flow

class SharingRepository(
    private val sharingDao: SharingDao,
    private val creditRepository: CreditRepository? = null
) {

    fun observeSharedPlans(userId: Long): Flow<List<SharedPlanEntity>> =
        sharingDao.getSharedPlansByUser(userId)

    suspend fun getSharedPlan(code: String): SharedPlanEntity? =
        sharingDao.getSharedPlanByCode(code)

    /**
     * Publishes a plan template share code, logs the SharedPlanEntity,
     * and automatically awards +25 credits to the sharing user.
     */
    suspend fun createAndSharePlan(
        planId: Long,
        planTitle: String,
        planDescription: String,
        durationDays: Int,
        templatePayloadJson: String,
        authorUserId: Long,
        remoteId: String? = null
    ): Pair<String, Uri> {
        val shareCode = PlanSharingManager.generateShareCode()

        val entity = SharedPlanEntity(
            shareCode = shareCode,
            originalPlanId = planId,
            authorUserId = authorUserId,
            planTitle = planTitle,
            planDescription = planDescription,
            durationDays = durationDays,
            templatePayloadJson = templatePayloadJson
        )

        sharingDao.insertSharedPlan(entity)

        // Award +25 credits to the sharing user
        creditRepository?.awardPlanShare(authorUserId, shareCode)

        val deepLinkUri = PlanSharingManager.buildDeepLinkUri(
            shareCode = shareCode,
            authorUserId = authorUserId,
            planTitle = planTitle,
            remoteId = remoteId
        )

        return Pair(shareCode, deepLinkUri)
    }

    suspend fun recordPlanClone(shareCode: String, recipientUserId: Long, authorUserId: Long) {
        sharingDao.incrementCloneCount(shareCode)

        // Award +100 bonus credits to creator when peer clones their shared plan
        creditRepository?.awardViralCloneBonus(
            creatorUserId = authorUserId,
            joinerUserId = recipientUserId,
            postId = shareCode,
            bonusPoints = 100
        )
    }
}
