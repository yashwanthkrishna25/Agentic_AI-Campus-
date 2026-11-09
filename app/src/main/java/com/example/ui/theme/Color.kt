package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// AGENTRA AI CAMPUS — Cyber Violet & Charcoal Design System Tokens
// =========================================================================

// Main Backgrounds & Surfaces (Dark Mode)
val CyberBg = Color(0xFF0F172A)           // Deep slate charcoal primary background
val CyberSurface = Color(0xFF111827)      // Charcoal surface
val CyberElevated = Color(0xFF172033)     // Elevated card container
val CyberBorder = Color(0xFF334155)       // Borders, dividers & subtle outlines

// Accents & Actions
val CyberViolet = Color(0xFF8B5CF6)       // Primary vivid cyber violet accent
val CyberVioletPressed = Color(0xFF7C3AED)// Pressed / active cyber violet
val CyberVioletGlow = Color(0x668B5CF6)   // Glow / shadow tone for buttons & cards
val CyberVioletSubtle = Color(0x268B5CF6) // 15% alpha background tint

// Text & Content (Dark Mode)
val TextIceBlue = Color(0xFFE2E8F0)       // Supporting soft ice blue text
val TextPrimary = Color(0xFFF8FAFC)       // Crisp white-slate high-emphasis
val TextSecondary = Color(0xFF94A3B8)     // Secondary slate text
val TextMuted = Color(0xFF64748B)         // Tertiary / muted slate text

// Semantic Status Indicators
val CyberSuccess = Color(0xFF22C55E)      // Success green
val CyberWarning = Color(0xFFF59E0B)      // Warning amber
val CyberError = Color(0xFFF87171)        // Error soft red
val CyberInfo = Color(0xFF38BDF8)         // Information sky blue

// Surface Container Levels (M3 Dark)
val SurfaceContainerLowest = Color(0xFF0A0F1D)
val SurfaceContainerLow = Color(0xFF0F172A)
val SurfaceContainer = Color(0xFF111827)
val SurfaceContainerHigh = Color(0xFF172033)
val SurfaceContainerHighest = Color(0xFF1E293B)

// Light Theme Surface Containers & Colors
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightElevated = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFCBD5E1)
val LightSurfaceLowest = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFF8FAFC)
val LightSurfaceContainer = Color(0xFFF1F5F9)
val LightSurfaceHigh = Color(0xFFE2E8F0)
val LightSurfaceHighest = Color(0xFFCBD5E1)
val LightPrimary = Color(0xFF8B5CF6)       // Cyber Violet remains brand primary in light mode
val LightPrimaryPressed = Color(0xFF7C3AED)
val LightSecondary = Color(0xFF6D28D9)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

// Backwards-Compatible Aliases mapped to the Cyber Violet & Charcoal Palette
val ObsidianBg = CyberBg
val ObsidianSurface = CyberSurface
val ObsidianElevated = CyberElevated
val ObsidianBorder = CyberBorder
val CyberEmerald = CyberViolet             // Re-routed to Cyber Violet identity
val CyberEmeraldDark = CyberVioletPressed
val ElectricIndigo = CyberViolet
val ElectricViolet = CyberViolet
val RoyalBlue = CyberInfo
val LuminousAmber = CyberWarning
val NeonAmber = CyberWarning
val NeonCoral = CyberError
val NeonGreen = CyberSuccess
val NeonBlue = CyberInfo
val CrimsonPrimary = CyberVioletPressed
val HeaderRed = CyberElevated
val LowRiskGreen = CyberSuccess
val ModerateRiskAmber = CyberWarning
val HighRiskRed = CyberError

// Role-Specific Identity Colors
val SuperAdminAccent = Color(0xFFA855F7)   // Purple Violet
val AdminAccent = Color(0xFFEC4899)        // Rose Pink
val PrincipalAccent = CyberWarning         // Luminous Amber
val HodAccent = Color(0xFF06B6D4)          // Cyan Teal
val FacultyAccent = CyberViolet            // Cyber Violet
val StudentAccent = Color(0xFF10B981)      // Emerald Green
val ParentAccent = CyberInfo               // Sky Blue

val CyberAccentPink = Color(0xFFEC4899)
val LightSurfaceVariant = Color(0xFFF1F5F9)
