import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';

// Import tool images
import timerImage from 'figma:asset/e370f7b04de7f21607fe11055c1412c627b06abb.png';
import tasksImage from 'figma:asset/5e266d324ad9a320d51d36f612bf18ed90e81365.png';
import goalsImage from 'figma:asset/3a03b0e47359b34faf37b039f3f5a1e8a47f0e61.png';
import notesImage from 'figma:asset/d16b9b5af834848418948dce1d516f3ef4dcd6e6.png';

// Background images
import bgTimerImage from 'figma:asset/d933993005e71243d441e36a9de41fd789536307.png';
import bgTasksImage from 'figma:asset/4f4b1f565398e18fb9f135cf2b3752ce75077394.png';
import bgNotesImage from 'figma:asset/f281d15255d91defffcd56cab7bfca88d53fb39f.png';
import bgGoalsImage from 'figma:asset/fc2b341558aa4d7502f45d6cfb79c069b6864421.png';

const tools = [
  { id: 1, name: 'Ekagra', image: timerImage },
  { id: 2, name: 'Mehfil', image: tasksImage },
  { id: 3, name: 'Nishtha', image: goalsImage },
  { id: 4, name: 'Dhyan', image: notesImage }
];

const showcaseCards = [
  { id: 1, title: 'Boost Your Productivity', description: 'Stay focused with you own podomoro timer and track your work sessions', feature: 'Ekagra', bgImage: bgTimerImage },
  { id: 2, title: 'Never Miss a Task', description: 'Organize your daily tasks with priority levels and reminders', feature: 'Mehfil', bgImage: bgTasksImage },
  { id: 3, title: 'Achieve Your Dreams', description: 'Set long-term goals and track your progress every day', feature: 'Nishtha', bgImage: bgGoalsImage },
  { id: 4, title: 'Capture Your Thoughts', description: 'Notes, ideas and reminders- All in one place', feature: 'Dhyan', bgImage: bgNotesImage },
];

export default function App() {
  const [activeTab, setActiveTab] = useState(0);

  return (
    <div className="size-full bg-gradient-to-b from-slate-900 to-slate-800 flex items-center justify-center p-4">
      <div className="w-full max-w-[400px] h-[800px] bg-black rounded-[50px] shadow-2xl overflow-hidden border-8 border-slate-900 relative">

        <div className="h-full relative overflow-hidden">

          {/* 🔥 BACKGROUND */}
          <div className="absolute inset-0">
            <AnimatePresence mode="sync">
              <motion.div
                key={activeTab}
                className="absolute inset-0"
                initial={{ opacity: 0, scale: 1.04 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.98 }}
                transition={{
                  opacity: { duration: 0.35 },
                  scale: { duration: 0.4 }
                }}
              >
                <motion.img
                  src={showcaseCards[activeTab].bgImage}
                  className="absolute inset-0 w-full h-full object-cover"
                  initial={{ scale: 1.1 }}
                  animate={{ scale: 1 }}
                  transition={{ duration: 0.6 }}
                />

                <div className="absolute inset-0 bg-gradient-to-b from-black/40 via-black/20 to-black/70" />
              </motion.div>
            </AnimatePresence>
          </div>

          {/* 🔥 GLASS TEXT */}
          <div className="absolute top-16 left-0 w-full px-6 text-white flex justify-center">
            <div className="w-full max-w-[85%] rounded-2xl bg-white/10 backdrop-blur-md border border-white/15 px-5 py-4 text-center shadow-[0_8px_30px_rgba(0,0,0,0.3)]">
              <p className="text-[11px] tracking-[0.25em] uppercase text-white/65 mb-2">
                {showcaseCards[activeTab].feature}
              </p>

              <h2 className="text-[28px] leading-[1.2] font-semibold mb-3 text-balance">
                {showcaseCards[activeTab].title}
              </h2>

              <p className="text-[14px] text-white/75 leading-relaxed text-balance">
                {showcaseCards[activeTab].description}
              </p>
            </div>
          </div>

          {/* 🔥 Tools with LIME AURA */}
          <div className="absolute bottom-42 left-0 right-0 px-4">
            <div className="flex justify-between items-center">
              {tools.map((tool, index) => (
                <motion.button
                  key={tool.id}
                  onClick={() => setActiveTab(index)}
                  whileTap={{ scale: 1.1 }}
                  whileHover={{ scale: 1.05 }}
                  className="flex flex-col items-center gap-1.5 w-[4.5rem] transition-all duration-300"
                >
                  <div
                    className={`relative rounded-[12px] p-[0.25rem] w-full h-[5.5rem] transition-all duration-300 ${
                      activeTab === index
                        ? "bg-gradient-to-br from-[#073B3A] to-[#052F2E] scale-[1.2] z-10 shadow-[0_0_20px_rgba(132,255,0,0.6),0_0_40px_rgba(132,255,0,0.25)]"
                        : "bg-gradient-to-br from-[#F7C85C] to-[#2e8b5f]"
                    }`}
                  >
                    <div className={`w-full h-full rounded-md backdrop-blur-lg border overflow-hidden ${
                      activeTab === index
                        ? "bg-white/10 border-lime-300/40 shadow-[inset_0_0_12px_rgba(132,255,0,0.25)]"
                        : "bg-white/10 border-white/15"
                    }`}>
                      <img src={tool.image} className="w-full h-full object-cover" />
                    </div>
                  </div>

                  <span className={`text-[11px] transition-all duration-300 ${
                    activeTab === index ? "text-white scale-105" : "text-slate-300"
                  }`}>
                    {tool.name}
                  </span>
                </motion.button>
              ))}
            </div>
          </div>

          {/* Button */}
          <div className="absolute bottom-24 left-0 right-0 px-10">
            <motion.button className="w-full bg-gradient-to-r from-[#3DAC78] to-[#073B3A] rounded-full py-2">
              <span className="text-white text-xs font-bold">
                GO TO DASHBOARD
              </span>
            </motion.button>
          </div>

        </div>
      </div>
    </div>
  );
}