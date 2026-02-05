package iss.nus.edu.sg.webviews.binitrightmobileapp.model

data class NewsItem(
    val newsId: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val status: String,
    val publishedDate: String? // Maps to LocalDateTime from Spring Boot
)