import SwiftUI

struct SectionHeader: View {
    let title: String

    var body: some View {
        HStack(spacing: 8) {
            Rectangle().fill(ApexColor.racingRed).frame(width: 4, height: 16)
            Text(title.uppercased()).font(ApexFont.label).tracking(1)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 20)
        .padding(.bottom, 8)
    }
}

struct TeamStripe: View {
    let color: Color
    var height: CGFloat = 28

    var body: some View {
        RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 4, height: height)
    }
}

struct PositionText: View {
    let position: Int
    var label: String?

    var body: some View {
        Text(label ?? "\(position)")
            .font(.system(size: 17, weight: .black).italic())
            .foregroundStyle(color)
            .frame(width: 32)
    }

    private var color: Color {
        switch position {
        case 1: ApexColor.gold
        case 2: ApexColor.silver
        case 3: ApexColor.bronze
        default: ApexColor.floodlight
        }
    }
}

struct StatTile: View {
    let label: String
    let value: String
    var accent: Color = ApexColor.floodlight

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(ApexFont.headline).foregroundStyle(accent)
                .minimumScaleFactor(0.6).lineLimit(1)
            Text(label.uppercased()).font(ApexFont.label).foregroundStyle(ApexColor.telemetry)
                .lineLimit(1).minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 14))
    }
}

/// Shown above cached content when the last refresh failed.
struct OfflineBanner: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        HStack {
            Image(systemName: "icloud.slash").foregroundStyle(ApexColor.telemetry)
            Text(message).font(.footnote)
            Spacer()
            Button("Réessayer", action: retry).font(ApexFont.label)
        }
        .padding(12)
        .background(ApexColor.carbonRaised, in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }
}

struct EmptyStateView: View {
    let message: String
    var retry: (() -> Void)?

    var body: some View {
        ContentUnavailableView {
            Label(message, systemImage: "flag.checkered")
        } actions: {
            if let retry {
                Button("Réessayer", action: retry).buttonStyle(.borderedProminent)
            }
        }
    }
}

/// A stylised racing helmet drawn in the team colours (no licensed artwork needed).
struct HelmetView: View {
    let color: Color
    var size: CGFloat = 96

    var body: some View {
        Canvas { context, canvas in
            let w = canvas.width, h = canvas.height
            var shell = Path()
            shell.move(to: CGPoint(x: w * 0.08, y: h * 0.62))
            shell.addCurve(to: CGPoint(x: w * 0.70, y: h * 0.12),
                           control1: CGPoint(x: w * 0.05, y: h * 0.20), control2: CGPoint(x: w * 0.45, y: h * 0.05))
            shell.addCurve(to: CGPoint(x: w * 0.95, y: h * 0.70),
                           control1: CGPoint(x: w * 0.92, y: h * 0.18), control2: CGPoint(x: w * 0.98, y: h * 0.45))
            shell.addLine(to: CGPoint(x: w * 0.90, y: h * 0.88))
            shell.addLine(to: CGPoint(x: w * 0.22, y: h * 0.88))
            shell.closeSubpath()
            context.fill(shell, with: .linearGradient(
                Gradient(colors: [color, color.opacity(0.55)]),
                startPoint: .zero, endPoint: CGPoint(x: w, y: h)))

            var visor = Path()
            visor.move(to: CGPoint(x: w * 0.38, y: h * 0.42))
            visor.addLine(to: CGPoint(x: w * 0.94, y: h * 0.42))
            visor.addLine(to: CGPoint(x: w * 0.95, y: h * 0.60))
            visor.addLine(to: CGPoint(x: w * 0.45, y: h * 0.60))
            visor.closeSubpath()
            context.fill(visor, with: .color(ApexColor.asphalt))

            context.fill(Path(CGRect(x: w * 0.10, y: h * 0.66, width: w * 0.80, height: h * 0.05)),
                         with: .color(.white.opacity(0.85)))
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}
