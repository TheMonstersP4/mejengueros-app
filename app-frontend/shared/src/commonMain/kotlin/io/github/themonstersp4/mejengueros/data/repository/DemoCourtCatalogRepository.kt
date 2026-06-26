package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository

class DemoCourtCatalogRepository : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(): List<CourtCatalogItem> = DemoCourtCatalogItems
}

private val DemoCourtCatalogItems =
    listOf(
        CourtCatalogItem(
            id = "court-1",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
            province = "San José",
            canton = "Escazú",
            surface = "Sintético",
            courtType = "Fútbol 5",
            imageUrl = null,
            isReservableToday = true,
            isPublished = true,
            isActive = true,
        ),
        CourtCatalogItem(
            id = "court-2",
            complexName = "Moravia FC",
            courtName = "Cancha A",
            province = "San José",
            canton = "Moravia",
            surface = "Sintético",
            courtType = "Fútbol 7",
            imageUrl = null,
            isReservableToday = true,
            isPublished = true,
            isActive = true,
        ),
        CourtCatalogItem(
            id = "court-3",
            complexName = "Complejo Curridabat",
            courtName = "Cancha 2",
            province = "San José",
            canton = "Curridabat",
            surface = "Híbrido",
            courtType = "Fútbol 5",
            imageUrl = null,
            isReservableToday = false,
            isPublished = true,
            isActive = true,
        ),
        CourtCatalogItem(
            id = "court-4",
            complexName = "Oculta",
            courtName = "Cancha B",
            province = "Cartago",
            canton = "Tres Ríos",
            surface = "Natural",
            courtType = "Fútbol 11",
            imageUrl = null,
            isReservableToday = false,
            isPublished = false,
            isActive = true,
        ),
        CourtCatalogItem(
            id = "court-5",
            complexName = "Grecia Arena",
            courtName = "Cancha Central",
            province = "Alajuela",
            canton = "Grecia",
            surface = "Híbrido",
            courtType = "Fútbol 5",
            imageUrl = null,
            isReservableToday = false,
            isPublished = true,
            isActive = true,
        ),
    )
