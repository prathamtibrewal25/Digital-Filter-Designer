package com.example.dsp

import kotlin.math.*

// Biquad represents a Second-Order Section (SOS) definition:
// H(z) = (b0 + b1*z^-1 + b2*z^-2) / (1 + a1*z^-1 + a2*z^-2)
data class Biquad(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double
) {
    fun evaluate(omega: Double): Complex {
        val cos1 = cos(omega)
        val sin1 = -sin(omega)
        val cos2 = cos(2.0 * omega)
        val sin2 = -sin(2.0 * omega)

        val z1 = Complex(cos1, sin1)
        val z2 = Complex(cos2, sin2)

        val num = Complex(b0, 0.0) + (z1 * b1) + (z2 * b2)
        val den = Complex(1.0, 0.0) + (z1 * a1) + (z2 * a2)

        return num / den
    }
}

// Stateful simulation for IIR Filter logic in Direct Form II
class BiquadFilter(val biquads: List<Biquad>) {
    private val w1 = DoubleArray(biquads.size)
    private val w2 = DoubleArray(biquads.size)

    fun processSample(input: Double): Double {
        var x = input
        for (i in biquads.indices) {
            val b = biquads[i]
            val w = x - b.a1 * w1[i] - b.a2 * w2[i]
            val y = b.b0 * w + b.b1 * w1[i] + b.b2 * w2[i]
            w2[i] = w1[i]
            w1[i] = w
            x = y
        }
        return x
    }

    fun reset() {
        w1.fill(0.0)
        w2.fill(0.0)
    }
}

enum class FilterType {
    LOWPASS, HIGHPASS, BANDPASS, BANDSTOP
}

enum class FilterMethod {
    FIR_WINDOW, IIR_BUTTERWORTH, IIR_CHEBYSHEV
}

enum class WindowType {
    RECTANGULAR, BARTLETT, HANN, HAMMING, BLACKMAN
}

data class FilterResult(
    val title: String,
    val method: FilterMethod,
    val type: FilterType,
    val sampleRate: Double,
    val fCutoff1: Double,
    val fCutoff2: Double,
    val isFir: Boolean,
    
    // Coefficients
    val firCoefficients: DoubleArray = doubleArrayOf(),
    val iirBiquads: List<Biquad> = emptyList(),
    
    // DSP Responses
    val impulseResponse: DoubleArray, // typically 100 points
    val freqResponseFreqs: DoubleArray, // 512 points (Hz)
    val freqResponseMagDb: DoubleArray, // 512 points (dB)
    val freqResponsePhaseDeg: DoubleArray // 512 points (degrees)
)

object FilterDesigner {

    fun generateResult(
        method: FilterMethod,
        type: FilterType,
        sampleRate: Double,
        fc1: Double,
        fc2: Double,
        firTaps: Int = 31,
        windowType: WindowType = WindowType.HAMMING,
        iirOrder: Int = 4,
        chebyRippleDb: Double = 1.0
    ): FilterResult {
        
        val isFir = method == FilterMethod.FIR_WINDOW
        val firCoefficients: DoubleArray
        val iirBiquads: List<Biquad>

        if (isFir) {
            firCoefficients = designFir(type, sampleRate, fc1, fc2, firTaps, windowType)
            iirBiquads = emptyList()
        } else {
            firCoefficients = doubleArrayOf()
            iirBiquads = designIir(method, type, sampleRate, fc1, fc2, iirOrder, chebyRippleDb)
        }

        // Compute response plots
        val impulsePoints = 100
        val impulseResponse = DoubleArray(impulsePoints)
        if (isFir) {
            // FIR impulse response is simply the coefficients themselves (zero-padded)
            for (i in 0 until impulsePoints) {
                impulseResponse[i] = if (i < firCoefficients.size) firCoefficients[i] else 0.0
            }
        } else {
            // IIR impulse response simulated numerically
            val simulator = BiquadFilter(iirBiquads)
            for (i in 0 until impulsePoints) {
                val input = if (i == 0) 1.0 else 0.0
                impulseResponse[i] = simulator.processSample(input)
            }
        }

        // Compute Frequency Response (DTFT): 512 points from 0 to fs/2
        val freqPoints = 512
        val freqs = DoubleArray(freqPoints)
        val magDbs = DoubleArray(freqPoints)
        val phases = DoubleArray(freqPoints)

        for (i in 0 until freqPoints) {
            val f = (i.toDouble() / (freqPoints - 1)) * (sampleRate / 2.0)
            freqs[i] = f
            val omega = 2 * PI * (f / sampleRate)

            val hOmega = if (isFir) {
                // DTFT of FIR coefficients
                var sumReal = 0.0
                var sumImag = 0.0
                for (n in firCoefficients.indices) {
                    val theta = -omega * n
                    sumReal += firCoefficients[n] * cos(theta)
                    sumImag += firCoefficients[n] * sin(theta)
                }
                Complex(sumReal, sumImag)
            } else {
                // Cascaded evaluation of SOS
                var h = Complex.ONE
                for (biquad in iirBiquads) {
                    h *= biquad.evaluate(omega)
                }
                h
            }

            // Magnitude in dB (and floor at -100 dB to avoid -infinity)
            val mag = hOmega.magnitude()
            val magDb = if (mag < 1e-5) -100.0 else max(-100.0, 20.0 * log10(mag))
            magDbs[i] = magDb

            // Phase in degrees
            phases[i] = hOmega.phase() * (180.0 / PI)
        }

        val designTitle = when (method) {
            FilterMethod.FIR_WINDOW -> "FIR ($windowType Window, $firTaps Taps)"
            FilterMethod.IIR_BUTTERWORTH -> "IIR Butterworth (Order $iirOrder)"
            FilterMethod.IIR_CHEBYSHEV -> "IIR Chebyshev ($chebyRippleDb dB Ripple, Order $iirOrder)"
        }

        return FilterResult(
            title = designTitle,
            method = method,
            type = type,
            sampleRate = sampleRate,
            fCutoff1 = fc1,
            fCutoff2 = fc2,
            isFir = isFir,
            firCoefficients = firCoefficients,
            iirBiquads = iirBiquads,
            impulseResponse = impulseResponse,
            freqResponseFreqs = freqs,
            freqResponseMagDb = magDbs,
            freqResponsePhaseDeg = phases
        )
    }

    // --- FIR WINDOW DESIGN METHOD ---
    private fun designFir(
        type: FilterType,
        fs: Double,
        fc1: Double,
        fc2: Double,
        taps: Int,
        windowType: WindowType
    ): DoubleArray {
        val h = DoubleArray(taps)
        val m = (taps - 1) / 2.0

        // Normalized cutoff frequencies
        val w1 = 2 * PI * (fc1 / fs)
        val w2 = 2 * PI * (fc2 / fs)

        for (n in 0 until taps) {
            val dist = n - m
            var hd = 0.0

            if (abs(dist) < 1e-9) {
                // Center sample
                hd = when (type) {
                    FilterType.LOWPASS -> w1 / PI
                    FilterType.HIGHPASS -> 1.0 - (w1 / PI)
                    FilterType.BANDPASS -> (w2 - w1) / PI
                    FilterType.BANDSTOP -> 1.0 - ((w2 - w1) / PI)
                }
            } else {
                hd = when (type) {
                    FilterType.LOWPASS -> sin(w1 * dist) / (PI * dist)
                    FilterType.HIGHPASS -> -sin(w1 * dist) / (PI * dist)
                    FilterType.BANDPASS -> (sin(w2 * dist) - sin(w1 * dist)) / (PI * dist)
                    FilterType.BANDSTOP -> (sin(w1 * dist) - sin(w2 * dist)) / (PI * dist)
                }
            }

            // Window value
            val w = when (windowType) {
                WindowType.RECTANGULAR -> 1.0
                WindowType.BARTLETT -> 1.0 - abs(dist / m)
                WindowType.HANN -> 0.5 * (1.0 - cos(2 * PI * n / (taps - 1)))
                WindowType.HAMMING -> 0.54 - 0.46 * cos(2 * PI * n / (taps - 1))
                WindowType.BLACKMAN -> 0.42 - 0.5 * cos(2 * PI * n / (taps - 1)) + 0.08 * cos(4 * PI * n / (taps - 1))
            }

            h[n] = hd * w
        }

        // DC or Passband Gain Normalization
        normalizeFirGain(h, type, w1, w2)
        return h
    }

    private fun normalizeFirGain(h: DoubleArray, type: FilterType, w1: Double, w2: Double) {
        // Find reference frequency in passband to set gain to exactly 0 dB (1.0)
        val testOmega = when (type) {
            FilterType.LOWPASS -> 0.0
            FilterType.HIGHPASS -> PI
            FilterType.BANDPASS -> (w1 + w2) / 2.0
            FilterType.BANDSTOP -> 0.0 // At 0 Hz or fs/2 it's passband, 0.0 is safe
        }

        var sumReal = 0.0
        var sumImag = 0.0
        for (n in h.indices) {
            sumReal += h[n] * cos(-testOmega * n)
            sumImag += h[n] * sin(-testOmega * n)
        }
        val gain = sqrt(sumReal * sumReal + sumImag * sumImag)
        if (gain > 1e-6) {
            for (n in h.indices) {
                h[n] /= gain
            }
        }
    }

    // --- IIR CASCDED BIQUAD DESIGN METHOD ---
    private fun designIir(
        method: FilterMethod,
        type: FilterType,
        fs: Double,
        fc1: Double,
        fc2: Double,
        order: Int,
        chebyRippleDb: Double
    ): List<Biquad> {
        val biquads = mutableListOf<Biquad>()
        val numSections = order / 2
        
        // Dynamic prewarping helper variables
        val wc1 = 2.0 * PI * (fc1 / fs)
        val omegaPrewarped1 = tan(wc1 / 2.0)
        
        val wc2 = 2.0 * PI * (fc2 / fs)
        val omegaPrewarped2 = tan(wc2 / 2.0)

        for (section in 0 until numSections) {
            // Find the normalized lowpass prototype pole (where cutoff frequency is 1.0 rad/s)
            val p = if (method == FilterMethod.IIR_BUTTERWORTH) {
                val theta = PI * (2 * section + 1) / (2 * order)
                Complex(-cos(theta), sin(theta))
            } else { // IIR_CHEBYSHEV
                val epsilon = sqrt(10.0.pow(chebyRippleDb / 10.0) - 1.0)
                val a = (1.0 / order) * asinh(1.0 / epsilon)
                val theta = PI * (2 * section + 1) / (2 * order)
                Complex(-sinh(a) * cos(theta), cosh(a) * sin(theta))
            }

            // Perform frequency transformations to map normalized lowpass prototype poles to physical S-plane
            when (type) {
                FilterType.LOWPASS -> {
                    val sPole = p * omegaPrewarped1
                    val c = -2.0 * sPole.real
                    val d = sPole.magnitude() * sPole.magnitude()
                    
                    val g = 1.0 + c + d
                    biquads.add(Biquad(
                        b0 = d / g,
                        b1 = 2.0 * d / g,
                        b2 = d / g,
                        a1 = (2.0 * d - 2.0) / g,
                        a2 = (1.0 - c + d) / g
                    ))
                }
                FilterType.HIGHPASS -> {
                    val sPole = Complex(omegaPrewarped1, 0.0) / p
                    val c = -2.0 * sPole.real
                    val d = sPole.magnitude() * sPole.magnitude()
                    
                    val g = 1.0 + c + d
                    biquads.add(Biquad(
                        b0 = 1.0 / g,
                        b1 = -2.0 / g,
                        b2 = 1.0 / g,
                        a1 = (2.0 * d - 2.0) / g,
                        a2 = (1.0 - c + d) / g
                    ))
                }
                FilterType.BANDPASS -> {
                    val bVal = omegaPrewarped2 - omegaPrewarped1
                    val w0Sq = omegaPrewarped1 * omegaPrewarped2
                    
                    val pB = p * bVal
                    val discriminant = (pB * pB) - Complex(4.0 * w0Sq, 0.0)
                    val discRoot = complexSqrt(discriminant)
                    
                    val s1 = (pB + discRoot) * 0.5
                    val s2 = (pB - discRoot) * 0.5
                    
                    for (sp in listOf(s1, s2)) {
                        val c = -2.0 * sp.real
                        val d = sp.magnitude() * sp.magnitude()
                        
                        val g = 1.0 + c + d
                        biquads.add(Biquad(
                            b0 = bVal / g,
                            b1 = 0.0,
                            b2 = -bVal / g,
                            a1 = (2.0 * d - 2.0) / g,
                            a2 = (1.0 - c + d) / g
                        ))
                    }
                }
                FilterType.BANDSTOP -> {
                    val bVal = omegaPrewarped2 - omegaPrewarped1
                    val w0Sq = omegaPrewarped1 * omegaPrewarped2
                    
                    val pRecip = Complex(1.0, 0.0) / p
                    val pBPrime = pRecip * bVal
                    val discriminant = (pBPrime * pBPrime) - Complex(4.0 * w0Sq, 0.0)
                    val discRoot = complexSqrt(discriminant)
                    
                    val s1 = (pBPrime + discRoot) * 0.5
                    val s2 = (pBPrime - discRoot) * 0.5
                    
                    for (sp in listOf(s1, s2)) {
                        val c = -2.0 * sp.real
                        val d = sp.magnitude() * sp.magnitude()
                        
                        val g = 1.0 + c + d
                        biquads.add(Biquad(
                            b0 = (1.0 + w0Sq) / g,
                            b1 = (2.0 * w0Sq - 2.0) / g,
                            b2 = (1.0 + w0Sq) / g,
                            a1 = (2.0 * d - 2.0) / g,
                            a2 = (1.0 - c + d) / g
                        ))
                    }
                }
            }
        }

        // Centralized Gain Normalization: Scale overall filter gain to exactly 0 dB (1.0) in the passband
        if (biquads.isNotEmpty()) {
            val referenceFreqHz = when (type) {
                FilterType.LOWPASS -> 0.0
                FilterType.HIGHPASS -> fs / 2.0
                FilterType.BANDPASS -> sqrt(fc1 * fc2)
                FilterType.BANDSTOP -> 0.0
            }
            val omegaRef = 2 * PI * (referenceFreqHz / fs)
            var currentGain = Complex.ONE
            for (b in biquads) {
                currentGain *= b.evaluate(omegaRef)
            }
            val magnitude = currentGain.magnitude()
            if (magnitude > 1e-4) {
                val scale = 1.0 / magnitude
                val b = biquads[0]
                biquads[0] = Biquad(
                    b0 = b.b0 * scale,
                    b1 = b.b1 * scale,
                    b2 = b.b2 * scale,
                    a1 = b.a1,
                    a2 = b.a2
                )
            }
        }

        return biquads
    }

    private fun complexSqrt(z: Complex): Complex {
        val r = z.magnitude()
        val realPart = sqrt((r + z.real) / 2.0)
        val imagPart = if (z.imag >= 0.0) sqrt((r - z.real) / 2.0) else -sqrt((r - z.real) / 2.0)
        return Complex(realPart, imagPart)
    }

    private fun asinh(x: Double): Double = ln(x + sqrt(x * x + 1.0))
}
