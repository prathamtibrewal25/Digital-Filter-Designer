package com.example.dsp

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Complex(val real: Double, val imag: Double) {
    operator fun plus(other: Complex) = Complex(real + other.real, imag + other.imag)
    operator fun minus(other: Complex) = Complex(real - other.real, imag - other.imag)
    operator fun times(other: Complex) = Complex(
        real * other.real - imag * other.imag,
        real * other.imag + imag * other.real
    )
    operator fun times(scalar: Double) = Complex(real * scalar, imag * scalar)
    operator fun div(scalar: Double) = Complex(real / scalar, imag / scalar)
    operator fun div(other: Complex): Complex {
        val denom = other.real * other.real + other.imag * other.imag
        if (denom == 0.0) return ZERO
        return Complex(
            (real * other.real + imag * other.imag) / denom,
            (imag * other.real - real * other.imag) / denom
        )
    }

    fun magnitude(): Double = sqrt(real * real + imag * imag)
    fun phase(): Double = atan2(imag, real)

    companion object {
        val ZERO = Complex(0.0, 0.0)
        val ONE = Complex(1.0, 0.0)
        
        fun fromPolar(r: Double, theta: Double) = Complex(r * cos(theta), r * sin(theta))
    }
}
