package com.example.mindlog.features.analysis.data.mapper


import com.example.mindlog.features.analysis.data.dto.ComprehensiveAnalysisResponse
import com.example.mindlog.features.analysis.data.dto.PersonalizedAdviceResponse
import com.example.mindlog.features.analysis.data.dto.UserTypeResponse
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import javax.inject.Inject

class AnalysisMapper @Inject constructor() {
    fun toUserType(res: UserTypeResponse) = UserType(
        userType = res.userType,
        updatedAt = res.updatedAt
    )

    fun toComprehensive(res: ComprehensiveAnalysisResponse) = ComprehensiveAnalysis(
        text = res.comprehensiveAnalysis,
        updatedAt = res.updatedAt
    )

    fun toAdvice(res: PersonalizedAdviceResponse) = PersonalizedAdvice(
        adviceType = res.adviceType,
        text = res.personalizedAdvice,
        updatedAt = res.updatedAt
    )

}