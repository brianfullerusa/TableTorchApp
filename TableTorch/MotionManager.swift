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
    private let updateInterval = 1.0 / 60.0  // 60 times/sec

    @Published var pitch: Double = 0.0
    @Published var roll: Double = 0.0
    @Published var brightnessTiltAngle: Double = 0.0
    

    func startUpdates() {
        guard motion.isDeviceMotionAvailable else { return }
        motion.deviceMotionUpdateInterval = updateInterval
        motion.startDeviceMotionUpdates(to: .main) { [weak self] deviceMotion, error in
            guard let self = self, let deviceMotion = deviceMotion else { return }
            self.pitch = deviceMotion.attitude.pitch
            self.roll = deviceMotion.attitude.roll

            let gravity = deviceMotion.gravity
            let clampedZ = max(-1.0, min(1.0, gravity.z))
            let downwardComponent = min(1.0, max(0.0, abs(clampedZ)))
            let tilt = acos(downwardComponent)
            self.brightnessTiltAngle = tilt

        }
    }

    func stopUpdates() {
        motion.stopDeviceMotionUpdates()
    }
}

