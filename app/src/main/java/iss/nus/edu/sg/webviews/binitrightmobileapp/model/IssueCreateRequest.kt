package iss.nus.edu.sg.webviews.binitrightmobileapp.model

data class IssueCreateRequest(
    val issueCategory: String,  // "BinIssues", "AppProblems", "LocationErrors", "Others"
    val description: String,
    val raisedByUserId: Long
)