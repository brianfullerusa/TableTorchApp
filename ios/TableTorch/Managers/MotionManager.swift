//
//  MotionManager.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI
import CoreMotion

class MotionManager: ObservableObject {
    private let motion = CMMotionManager()
    private static let brightnessInterval = 1.0 / 60.0  // 60 Hz for smooth brightness
    private static let defaultInterval = 1.0 / 20.0     // 20 Hz for general motion
    private let motionQueue: OperationQueue = {
        let queue = OperationQueue()
        queue.name = "com.tabletorch.motion"
        queue.maxConcurrentOperationCount = 1
        return queue
    }()
    private var brightnessControlActive = false

    @Published var brightnessTiltAngle: Double = 0.0
    @Published private(set) var isMotionAvailable: Bool = CMMotionManager().isDeviceMotionAvailable

    deinit {
        motion.stopDeviceMotionUpdates()
    }

    func startUpdates(brightnessControl: Bool = false) {
        brightnessControlActive = brightnessControl
        isMotionAvailable = motion.isDeviceMotionAvailable
        guard motion.isDeviceMotionAvailable else { return }

        // Restart with the appropriate interval
        motion.stopDeviceMotionUpdates()
        motion.deviceMotionUpdateInterval = brightnessControl
            ? Self.brightnessInterval
            : Self.defaultInterval
        motion.startDeviceMotionUpdates(to: motionQueue) { [weak self] deviceMotion, error in
            guard let self = self, let deviceMotion = deviceMotion else { return }

            let gravity = deviceMotion.gravity
            let clampedZ = max(-1.0, min(1.0, gravity.z))
            let downwardComponent = min(1.0, max(0.0, abs(clampedZ)))
            let tilt = acos(downwardComponent)

            DispatchQueue.main.async {
                self.brightnessTiltAngle = tilt
            }
        }
    }

    func stopUpdates() {
        brightnessControlActive = false
        motion.stopDeviceMotionUpdates()
    }
}

