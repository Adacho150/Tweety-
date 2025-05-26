package com.example.tweety2

data class FeatureCollection(
    val type: String,
    val features: List<Feature>
)

data class Feature(
    val type: String,
    val geometry: Geometry,
    val properties: Map<String, String>
)

data class Geometry(
    val type: String,
    val coordinates: List<Double>
)