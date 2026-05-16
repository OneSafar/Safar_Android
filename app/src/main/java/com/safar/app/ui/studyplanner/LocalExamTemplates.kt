package com.safar.app.ui.studyplanner

import com.safar.app.domain.model.studyplanner.ExamTemplate
import com.safar.app.domain.model.studyplanner.TemplateChapter
import com.safar.app.domain.model.studyplanner.TemplateSubject

fun getLocalExamTemplate(id: String): ExamTemplate? = localExamTemplates.firstOrNull { it.id == id }

private val localExamTemplates = listOf(
    ExamTemplate(
        id = "ssc-cgl-tier1",
        name = "SSC CGL Tier-1",
        description = "Combined Graduate Level Examination Tier-1 - 100 questions, 200 marks, 60 minutes",
        recommendedDailyGoal = 4,
        subjects = listOf(
            TemplateSubject(
                name = "Quantitative Aptitude",
                color = "#3b82f6",
                chapters = listOf(
                    TemplateChapter(
                        name = "Number System",
                        topics = listOf(
                            "LCM & HCF",
                            "Divisibility Rules",
                            "Remainder Theorem",
                            "Unit Digit & Last Two Digits",
                            "Factors & Multiples",
                            "Prime & Composite Numbers",
                            "Fraction & Decimals",
                        ),
                    ),
                    TemplateChapter(
                        name = "Simplification & Approximation",
                        topics = listOf(
                            "BODMAS Rule",
                            "Surds & Indices",
                            "Square Root & Cube Root",
                            "Approximation Techniques",
                        ),
                    ),
                    TemplateChapter(
                        name = "Percentage",
                        topics = listOf(
                            "Basic Percentage Problems",
                            "Percentage Change",
                            "Successive Percentage",
                            "Population & Depreciation Problems",
                        ),
                    ),
                    TemplateChapter(
                        name = "Ratio & Proportion",
                        topics = listOf(
                            "Simple Ratio",
                            "Compound Ratio",
                            "Proportion & Variation",
                            "Partnership Problems",
                        ),
                    ),
                    TemplateChapter(
                        name = "Average",
                        topics = listOf(
                            "Basic Average",
                            "Weighted Average",
                            "Average Speed",
                            "Age-based Average Problems",
                        ),
                    ),
                    TemplateChapter(
                        name = "Profit, Loss & Discount",
                        topics = listOf(
                            "Basic Profit & Loss",
                            "Successive Discounts",
                            "Marked Price & Selling Price",
                            "Dishonest Dealer Problems",
                            "Mixture Problems in Profit/Loss",
                        ),
                    ),
                    TemplateChapter(
                        name = "Simple & Compound Interest",
                        topics = listOf(
                            "Simple Interest",
                            "Compound Interest",
                            "Difference Between SI & CI",
                            "Installment Problems",
                        ),
                    ),
                    TemplateChapter(
                        name = "Time & Work",
                        topics = listOf(
                            "Basic Time & Work",
                            "Pipes & Cisterns",
                            "Work & Wages",
                            "Alternate Day Problems",
                            "Efficiency-based Problems",
                        ),
                    ),
                    TemplateChapter(
                        name = "Speed, Time & Distance",
                        topics = listOf(
                            "Basic Speed Problems",
                            "Relative Speed",
                            "Trains",
                            "Boats & Streams",
                            "Circular Motion & Races",
                        ),
                    ),
                    TemplateChapter(
                        name = "Algebra",
                        topics = listOf(
                            "Linear Equations",
                            "Quadratic Equations",
                            "Algebraic Identities",
                            "Polynomials & Factorization",
                        ),
                    ),
                    TemplateChapter(
                        name = "Geometry",
                        topics = listOf(
                            "Lines & Angles",
                            "Triangles - Properties & Theorems",
                            "Circles - Chords, Tangents, Arcs",
                            "Quadrilaterals & Polygons",
                            "Coordinate Geometry Basics",
                        ),
                    ),
                    TemplateChapter(
                        name = "Mensuration",
                        topics = listOf(
                            "Area - Triangle, Circle, Quadrilateral",
                            "Volume - Cube, Cuboid, Cylinder",
                            "Volume - Cone, Sphere, Hemisphere",
                            "Surface Area Problems",
                            "Combined Solid Problems",
                        ),
                    ),
                    TemplateChapter(
                        name = "Trigonometry",
                        topics = listOf(
                            "Trigonometric Ratios & Identities",
                            "Complementary Angles",
                            "Height & Distance",
                            "Maximum & Minimum Values",
                        ),
                    ),
                    TemplateChapter(
                        name = "Data Interpretation",
                        topics = listOf(
                            "Bar Graph",
                            "Pie Chart",
                            "Line Graph",
                            "Table-based DI",
                            "Mixed DI",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "English Language",
                color = "#10b981",
                chapters = listOf(
                    TemplateChapter(
                        name = "Reading Comprehension",
                        topics = listOf(
                            "Passage-based Questions",
                            "Inference & Conclusion",
                            "Vocabulary in Context",
                            "Author's Tone & Purpose",
                        ),
                    ),
                    TemplateChapter(
                        name = "Grammar",
                        topics = listOf(
                            "Tenses",
                            "Subject-Verb Agreement",
                            "Articles & Determiners",
                            "Prepositions",
                            "Conjunctions",
                            "Modals & Auxiliaries",
                            "Direct & Indirect Speech",
                            "Active & Passive Voice",
                            "Conditionals",
                        ),
                    ),
                    TemplateChapter(
                        name = "Vocabulary",
                        topics = listOf(
                            "Synonyms & Antonyms",
                            "One Word Substitution",
                            "Idioms & Phrases",
                            "Phrasal Verbs",
                            "Spelling Errors",
                        ),
                    ),
                    TemplateChapter(
                        name = "Error Detection & Correction",
                        topics = listOf(
                            "Spotting Errors",
                            "Sentence Correction",
                            "Sentence Improvement",
                        ),
                    ),
                    TemplateChapter(
                        name = "Sentence Arrangement",
                        topics = listOf(
                            "Para Jumbles",
                            "Cloze Test",
                            "Fill in the Blanks",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "General Intelligence & Reasoning",
                color = "#f59e0b",
                chapters = listOf(
                    TemplateChapter(
                        name = "Verbal Reasoning",
                        topics = listOf(
                            "Analogy",
                            "Classification (Odd One Out)",
                            "Series - Number & Alphabet",
                            "Coding-Decoding",
                            "Blood Relations",
                            "Direction & Distance",
                            "Order & Ranking",
                            "Seating Arrangement - Linear",
                            "Seating Arrangement - Circular",
                            "Syllogism",
                        ),
                    ),
                    TemplateChapter(
                        name = "Non-Verbal Reasoning",
                        topics = listOf(
                            "Figure Series",
                            "Mirror & Water Image",
                            "Paper Cutting & Folding",
                            "Embedded Figures",
                            "Counting Figures",
                            "Dice & Cubes",
                        ),
                    ),
                    TemplateChapter(
                        name = "Logical Reasoning",
                        topics = listOf(
                            "Statement & Conclusion",
                            "Statement & Assumption",
                            "Cause & Effect",
                            "Venn Diagrams",
                            "Mathematical Operations",
                            "Puzzles",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "General Awareness",
                color = "#8b5cf6",
                chapters = listOf(
                    TemplateChapter(
                        name = "History",
                        topics = listOf(
                            "Ancient India - Indus Valley to Gupta",
                            "Medieval India - Delhi Sultanate & Mughals",
                            "Modern India - British Rule & Freedom Struggle",
                            "Art & Culture",
                            "Important Dates & Events",
                        ),
                    ),
                    TemplateChapter(
                        name = "Geography",
                        topics = listOf(
                            "Physical Geography - Landforms & Climate",
                            "Indian Geography - Rivers, Soils, Agriculture",
                            "World Geography - Continents & Oceans",
                            "Economic Geography - Industries & Trade",
                            "Maps & Important Places",
                        ),
                    ),
                    TemplateChapter(
                        name = "Polity",
                        topics = listOf(
                            "Indian Constitution - Preamble & Features",
                            "Fundamental Rights & Duties",
                            "Parliament & State Legislature",
                            "Judiciary - Supreme Court & High Court",
                            "Local Self Government",
                            "Constitutional Amendments",
                        ),
                    ),
                    TemplateChapter(
                        name = "Economics",
                        topics = listOf(
                            "Basic Economic Concepts",
                            "Indian Economy - Planning & Reforms",
                            "Banking & Finance - RBI, SEBI, NABARD",
                            "Budget & Fiscal Policy",
                            "International Organizations - IMF, WTO, World Bank",
                            "Government Schemes",
                        ),
                    ),
                    TemplateChapter(
                        name = "General Science",
                        topics = listOf(
                            "Physics - Mechanics, Heat, Light, Sound",
                            "Chemistry - Elements, Compounds, Reactions",
                            "Biology - Human Body Systems",
                            "Biology - Diseases & Nutrition",
                            "Science & Technology - Current Developments",
                            "Space & Defense Technology",
                        ),
                    ),
                    TemplateChapter(
                        name = "Static GK & Current Affairs",
                        topics = listOf(
                            "Books & Authors",
                            "Awards & Honours",
                            "Sports - Tournaments & Records",
                            "National & International Organizations",
                            "Current Affairs - Last 6 Months",
                        ),
                    ),
                ),
            ),
        ),
    ),
    ExamTemplate(
        id = "railway-ntpc",
        name = "Railway NTPC CBT-1",
        description = "Non-Technical Popular Categories CBT-1 - 100 questions, 100 marks, 90 minutes",
        recommendedDailyGoal = 4,
        subjects = listOf(
            TemplateSubject(
                name = "Mathematics",
                color = "#3b82f6",
                chapters = listOf(
                    TemplateChapter(
                        name = "Number System",
                        topics = listOf(
                            "LCM & HCF",
                            "Divisibility & Remainder",
                            "Fractions & Decimals",
                            "Square Root & Cube Root",
                        ),
                    ),
                    TemplateChapter(
                        name = "Simplification",
                        topics = listOf(
                            "BODMAS",
                            "Surds & Indices",
                            "Approximation",
                        ),
                    ),
                    TemplateChapter(
                        name = "Percentage",
                        topics = listOf(
                            "Basic Percentage",
                            "Percentage Change & Applications",
                        ),
                    ),
                    TemplateChapter(
                        name = "Ratio, Proportion & Partnership",
                        topics = listOf(
                            "Ratio & Proportion",
                            "Partnership",
                        ),
                    ),
                    TemplateChapter(
                        name = "Average & Ages",
                        topics = listOf(
                            "Average",
                            "Problems on Ages",
                        ),
                    ),
                    TemplateChapter(
                        name = "Profit, Loss & Discount",
                        topics = listOf(
                            "Profit & Loss Basics",
                            "Discount & Marked Price",
                            "Successive Discounts",
                        ),
                    ),
                    TemplateChapter(
                        name = "Simple & Compound Interest",
                        topics = listOf(
                            "SI Problems",
                            "CI Problems",
                            "Difference SI vs CI",
                        ),
                    ),
                    TemplateChapter(
                        name = "Time & Work",
                        topics = listOf(
                            "Time & Work",
                            "Pipes & Cisterns",
                        ),
                    ),
                    TemplateChapter(
                        name = "Speed, Time & Distance",
                        topics = listOf(
                            "Speed & Distance Basics",
                            "Trains",
                            "Boats & Streams",
                        ),
                    ),
                    TemplateChapter(
                        name = "Algebra",
                        topics = listOf(
                            "Linear Equations",
                            "Algebraic Identities",
                        ),
                    ),
                    TemplateChapter(
                        name = "Geometry & Mensuration",
                        topics = listOf(
                            "Triangles & Circles",
                            "Area of 2D Shapes",
                            "Volume of 3D Shapes",
                            "Surface Area",
                        ),
                    ),
                    TemplateChapter(
                        name = "Trigonometry",
                        topics = listOf(
                            "Trigonometric Ratios",
                            "Height & Distance",
                        ),
                    ),
                    TemplateChapter(
                        name = "Data Interpretation",
                        topics = listOf(
                            "Bar & Pie Chart",
                            "Tables & Line Graph",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "General Intelligence & Reasoning",
                color = "#f59e0b",
                chapters = listOf(
                    TemplateChapter(
                        name = "Verbal Reasoning",
                        topics = listOf(
                            "Analogy",
                            "Classification",
                            "Series Completion",
                            "Coding-Decoding",
                            "Blood Relations",
                            "Direction Sense",
                            "Order Ranking",
                            "Seating Arrangement",
                            "Syllogism",
                        ),
                    ),
                    TemplateChapter(
                        name = "Non-Verbal Reasoning",
                        topics = listOf(
                            "Figure Series & Pattern",
                            "Mirror & Water Image",
                            "Paper Folding & Cutting",
                            "Embedded & Counting Figures",
                            "Dice Problems",
                        ),
                    ),
                    TemplateChapter(
                        name = "Logical Reasoning",
                        topics = listOf(
                            "Venn Diagrams",
                            "Statement & Conclusion",
                            "Mathematical Operations",
                            "Puzzles & Arrangement",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "General Awareness",
                color = "#8b5cf6",
                chapters = listOf(
                    TemplateChapter(
                        name = "History",
                        topics = listOf(
                            "Ancient India",
                            "Medieval India",
                            "Modern India & Freedom Struggle",
                            "Indian Art & Culture",
                        ),
                    ),
                    TemplateChapter(
                        name = "Geography",
                        topics = listOf(
                            "Indian Physical Geography",
                            "Indian Rivers & Climate",
                            "World Geography Basics",
                            "Natural Resources & Agriculture",
                        ),
                    ),
                    TemplateChapter(
                        name = "Indian Polity",
                        topics = listOf(
                            "Constitution - Preamble & Features",
                            "Fundamental Rights & DPSP",
                            "Parliament & State Legislature",
                            "Judiciary & Local Government",
                        ),
                    ),
                    TemplateChapter(
                        name = "Indian Economy",
                        topics = listOf(
                            "Economic Planning & Five Year Plans",
                            "Banking - RBI & Monetary Policy",
                            "Budget & Fiscal Policy",
                            "Government Schemes & Programs",
                        ),
                    ),
                    TemplateChapter(
                        name = "General Science",
                        topics = listOf(
                            "Physics Basics",
                            "Chemistry Basics",
                            "Biology - Human Body & Diseases",
                            "Everyday Science & Technology",
                        ),
                    ),
                    TemplateChapter(
                        name = "Static GK & Current Affairs",
                        topics = listOf(
                            "Indian Railways - Facts & History",
                            "National Symbols & Honors",
                            "Books, Authors & Awards",
                            "Sports & Important Days",
                            "Current Affairs - Last 6 Months",
                        ),
                    ),
                ),
            ),
        ),
    ),
    ExamTemplate(
        id = "bank-po-prelims",
        name = "Bank PO Prelims",
        description = "IBPS PO / SBI PO Preliminary Exam - 100 questions, 100 marks, 60 minutes",
        recommendedDailyGoal = 3,
        subjects = listOf(
            TemplateSubject(
                name = "Quantitative Aptitude",
                color = "#3b82f6",
                chapters = listOf(
                    TemplateChapter(
                        name = "Number System & Simplification",
                        topics = listOf(
                            "LCM, HCF & Divisibility",
                            "BODMAS & Simplification",
                            "Surds & Indices",
                            "Approximation",
                        ),
                    ),
                    TemplateChapter(
                        name = "Percentage & Ratio",
                        topics = listOf(
                            "Percentage",
                            "Ratio & Proportion",
                            "Partnership",
                        ),
                    ),
                    TemplateChapter(
                        name = "Average & Ages",
                        topics = listOf(
                            "Average",
                            "Problems on Ages",
                        ),
                    ),
                    TemplateChapter(
                        name = "Profit, Loss & Interest",
                        topics = listOf(
                            "Profit & Loss",
                            "Discount",
                            "Simple Interest",
                            "Compound Interest",
                        ),
                    ),
                    TemplateChapter(
                        name = "Time & Work",
                        topics = listOf(
                            "Time & Work Basics",
                            "Pipes & Cisterns",
                        ),
                    ),
                    TemplateChapter(
                        name = "Speed, Time & Distance",
                        topics = listOf(
                            "Speed & Distance",
                            "Trains",
                            "Boats & Streams",
                        ),
                    ),
                    TemplateChapter(
                        name = "Algebra",
                        topics = listOf(
                            "Linear & Quadratic Equations",
                            "Inequalities",
                        ),
                    ),
                    TemplateChapter(
                        name = "Mensuration & Geometry",
                        topics = listOf(
                            "Area & Perimeter",
                            "Volume & Surface Area",
                        ),
                    ),
                    TemplateChapter(
                        name = "Data Interpretation",
                        topics = listOf(
                            "Bar Graph & Pie Chart",
                            "Line Graph & Table DI",
                            "Caselet DI",
                            "Mixed DI",
                        ),
                    ),
                    TemplateChapter(
                        name = "Number Series",
                        topics = listOf(
                            "Missing Number in Series",
                            "Wrong Number in Series",
                        ),
                    ),
                    TemplateChapter(
                        name = "Data Sufficiency",
                        topics = listOf(
                            "Quantitative Data Sufficiency",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "Reasoning Ability",
                color = "#f59e0b",
                chapters = listOf(
                    TemplateChapter(
                        name = "Arrangement & Puzzles",
                        topics = listOf(
                            "Linear Seating Arrangement",
                            "Circular Seating Arrangement",
                            "Floor-based Puzzles",
                            "Box-based Puzzles",
                            "Scheduling & Day-based Puzzles",
                        ),
                    ),
                    TemplateChapter(
                        name = "Coding-Decoding & Series",
                        topics = listOf(
                            "New Pattern Coding-Decoding",
                            "Alphabet & Number Series",
                            "Input-Output Machine",
                        ),
                    ),
                    TemplateChapter(
                        name = "Logical Reasoning",
                        topics = listOf(
                            "Syllogism",
                            "Inequality (Coded & Direct)",
                            "Blood Relations",
                            "Direction & Distance",
                            "Order & Ranking",
                        ),
                    ),
                    TemplateChapter(
                        name = "Miscellaneous",
                        topics = listOf(
                            "Alphanumeric Series",
                            "Data Sufficiency - Reasoning",
                            "Verbal Reasoning",
                            "Critical Reasoning",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "English Language",
                color = "#10b981",
                chapters = listOf(
                    TemplateChapter(
                        name = "Reading Comprehension",
                        topics = listOf(
                            "RC - Factual Passages",
                            "RC - Inference Based",
                            "Vocabulary from RC",
                        ),
                    ),
                    TemplateChapter(
                        name = "Grammar & Error Detection",
                        topics = listOf(
                            "Spotting Errors",
                            "Sentence Correction",
                            "Phrase Replacement",
                        ),
                    ),
                    TemplateChapter(
                        name = "Vocabulary",
                        topics = listOf(
                            "Synonyms & Antonyms",
                            "Idioms & Phrases",
                            "One Word Substitution",
                        ),
                    ),
                    TemplateChapter(
                        name = "Sentence Arrangement",
                        topics = listOf(
                            "Para Jumbles",
                            "Cloze Test",
                            "Fill in the Blanks - Single & Double",
                        ),
                    ),
                    TemplateChapter(
                        name = "Miscellaneous",
                        topics = listOf(
                            "Word Usage & Column-based",
                            "Sentence Connectors",
                            "Odd Sentence Out",
                        ),
                    ),
                ),
            ),
        ),
    ),
    ExamTemplate(
        id = "jee-mains",
        name = "JEE Mains",
        description = "Joint Entrance Examination (Main) - 90 questions across Physics, Chemistry, and Mathematics. 300 marks, 3 hours.",
        recommendedDailyGoal = 4,
        subjects = listOf(
            TemplateSubject(
                name = "Physics",
                color = "#3b82f6",
                chapters = listOf(
                    TemplateChapter(
                        name = "Units & Measurements",
                        topics = listOf(
                            "SI Units & Dimensional Analysis",
                            "Significant Figures & Errors",
                            "Measurement Instruments",
                        ),
                    ),
                    TemplateChapter(
                        name = "Kinematics",
                        topics = listOf(
                            "Motion in a Straight Line",
                            "Motion in a Plane",
                            "Projectile Motion",
                            "Relative Motion",
                        ),
                    ),
                    TemplateChapter(
                        name = "Laws of Motion",
                        topics = listOf(
                            "Newton's Laws of Motion",
                            "Friction - Static & Kinetic",
                            "Circular Motion & Banking",
                            "Pseudo Forces & Non-Inertial Frames",
                        ),
                    ),
                    TemplateChapter(
                        name = "Work, Energy & Power",
                        topics = listOf(
                            "Work-Energy Theorem",
                            "Conservation of Energy",
                            "Power & Collisions",
                            "Potential Energy Functions",
                        ),
                    ),
                    TemplateChapter(
                        name = "Rotational Motion",
                        topics = listOf(
                            "Moment of Inertia",
                            "Torque & Angular Momentum",
                            "Rolling Motion",
                            "Equilibrium of Rigid Bodies",
                        ),
                    ),
                    TemplateChapter(
                        name = "Gravitation",
                        topics = listOf(
                            "Newton's Law of Gravitation",
                            "Gravitational Potential & Field",
                            "Orbital & Escape Velocity",
                            "Kepler's Laws & Satellites",
                        ),
                    ),
                    TemplateChapter(
                        name = "Properties of Solids & Fluids",
                        topics = listOf(
                            "Elasticity & Stress-Strain",
                            "Fluid Pressure & Pascal's Law",
                            "Bernoulli's Principle",
                            "Viscosity & Surface Tension",
                        ),
                    ),
                    TemplateChapter(
                        name = "Thermodynamics",
                        topics = listOf(
                            "Laws of Thermodynamics",
                            "Heat Transfer - Conduction, Convection, Radiation",
                            "Kinetic Theory of Gases",
                            "Thermal Expansion & Calorimetry",
                            "PV Diagrams & Thermodynamic Processes",
                        ),
                    ),
                    TemplateChapter(
                        name = "Oscillations & Waves",
                        topics = listOf(
                            "Simple Harmonic Motion",
                            "Damped & Forced Oscillations",
                            "Transverse & Longitudinal Waves",
                            "Superposition & Standing Waves",
                            "Doppler Effect",
                        ),
                    ),
                    TemplateChapter(
                        name = "Electrostatics",
                        topics = listOf(
                            "Coulomb's Law & Electric Field",
                            "Gauss's Law & Applications",
                            "Electric Potential & Capacitance",
                            "Dielectrics & Energy Stored",
                        ),
                    ),
                    TemplateChapter(
                        name = "Current Electricity",
                        topics = listOf(
                            "Ohm's Law & Resistivity",
                            "Kirchhoff's Laws & Circuits",
                            "Wheatstone Bridge & Potentiometer",
                            "RC Circuits & Electrical Energy",
                        ),
                    ),
                    TemplateChapter(
                        name = "Magnetic Effects of Current & Magnetism",
                        topics = listOf(
                            "Biot-Savart Law & Ampere's Law",
                            "Force on Moving Charge & Current",
                            "Magnetic Properties of Materials",
                            "Earth's Magnetism",
                        ),
                    ),
                    TemplateChapter(
                        name = "Electromagnetic Induction & AC",
                        topics = listOf(
                            "Faraday's Law & Lenz's Law",
                            "Self & Mutual Inductance",
                            "AC Circuits - LCR, Resonance",
                            "Transformers & Power Transmission",
                        ),
                    ),
                    TemplateChapter(
                        name = "Optics & Modern Physics",
                        topics = listOf(
                            "Ray Optics - Reflection & Refraction",
                            "Wave Optics - Interference & Diffraction",
                            "Photoelectric Effect",
                            "Atomic Models - Bohr's Theory",
                            "Nuclear Physics & Radioactivity",
                            "Semiconductor Devices - Diode & Transistor",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "Chemistry",
                color = "#10b981",
                chapters = listOf(
                    TemplateChapter(
                        name = "Atomic Structure",
                        topics = listOf(
                            "Bohr Model & Quantum Numbers",
                            "Electronic Configuration & Orbitals",
                            "Photoelectric Effect & Spectra",
                        ),
                    ),
                    TemplateChapter(
                        name = "Chemical Bonding",
                        topics = listOf(
                            "Ionic & Covalent Bonding",
                            "VSEPR Theory & Hybridization",
                            "Molecular Orbital Theory",
                            "Hydrogen Bonding & Metallic Bonding",
                        ),
                    ),
                    TemplateChapter(
                        name = "States of Matter",
                        topics = listOf(
                            "Gas Laws & Kinetic Theory",
                            "Liquid State & Vapour Pressure",
                            "Solid State - Crystal Structures & Defects",
                        ),
                    ),
                    TemplateChapter(
                        name = "Thermodynamics & Thermochemistry",
                        topics = listOf(
                            "Enthalpy & Hess's Law",
                            "Entropy & Gibbs Free Energy",
                            "Spontaneity & Equilibrium",
                        ),
                    ),
                    TemplateChapter(
                        name = "Chemical Equilibrium",
                        topics = listOf(
                            "Le Chatelier's Principle",
                            "Equilibrium Constants - Kp, Kc",
                            "Ionic Equilibrium - pH & Buffers",
                            "Solubility Product",
                        ),
                    ),
                    TemplateChapter(
                        name = "Electrochemistry",
                        topics = listOf(
                            "Nernst Equation & Cell Potential",
                            "Electrolysis & Faraday's Laws",
                            "Conductance & Kohlrausch's Law",
                            "Batteries & Corrosion",
                        ),
                    ),
                    TemplateChapter(
                        name = "Chemical Kinetics",
                        topics = listOf(
                            "Rate Law & Order of Reaction",
                            "Arrhenius Equation & Activation Energy",
                            "Integrated Rate Equations",
                            "Catalysis",
                        ),
                    ),
                    TemplateChapter(
                        name = "Solutions",
                        topics = listOf(
                            "Raoult's Law & Colligative Properties",
                            "Osmotic Pressure",
                            "Abnormal Molecular Mass - Van't Hoff Factor",
                        ),
                    ),
                    TemplateChapter(
                        name = "Periodic Table & Classification",
                        topics = listOf(
                            "Periodic Trends - Electronegativity, IE, EA",
                            "s-Block Elements",
                            "p-Block Elements - Groups 13-18",
                            "d-Block & f-Block Elements",
                            "Coordination Compounds",
                        ),
                    ),
                    TemplateChapter(
                        name = "Organic Chemistry - Basics",
                        topics = listOf(
                            "IUPAC Nomenclature",
                            "Isomerism - Structural & Stereoisomerism",
                            "Electronic Effects - Inductive, Resonance, Hyperconjugation",
                            "Reaction Intermediates - Carbocation, Carbanion, Free Radicals",
                        ),
                    ),
                    TemplateChapter(
                        name = "Hydrocarbons",
                        topics = listOf(
                            "Alkanes - Preparation & Reactions",
                            "Alkenes - Addition Reactions",
                            "Alkynes - Preparation & Properties",
                            "Aromatic Hydrocarbons - Benzene & EAS",
                        ),
                    ),
                    TemplateChapter(
                        name = "Organic Functional Groups",
                        topics = listOf(
                            "Alcohols, Phenols & Ethers",
                            "Aldehydes & Ketones",
                            "Carboxylic Acids & Derivatives",
                            "Amines & Diazonium Salts",
                            "Haloarenes & Haloalkanes",
                        ),
                    ),
                    TemplateChapter(
                        name = "Biomolecules & Polymers",
                        topics = listOf(
                            "Carbohydrates - Mono & Polysaccharides",
                            "Amino Acids & Proteins",
                            "Nucleic Acids - DNA & RNA",
                            "Polymers - Addition & Condensation",
                            "Chemistry in Everyday Life",
                        ),
                    ),
                    TemplateChapter(
                        name = "Surface Chemistry & General Principles",
                        topics = listOf(
                            "Adsorption & Catalysis",
                            "Colloids & Emulsions",
                            "Metallurgy - Extraction Principles",
                            "Environmental Chemistry",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "Mathematics",
                color = "#f59e0b",
                chapters = listOf(
                    TemplateChapter(
                        name = "Sets, Relations & Functions",
                        topics = listOf(
                            "Types of Sets & Venn Diagrams",
                            "Relations - Equivalence & Partial Order",
                            "Functions - Domain, Range, Types",
                            "Composition & Inverse of Functions",
                        ),
                    ),
                    TemplateChapter(
                        name = "Complex Numbers & Quadratic Equations",
                        topics = listOf(
                            "Algebra of Complex Numbers",
                            "Argand Plane & Polar Form",
                            "Quadratic Equations & Discriminant",
                            "Nature of Roots",
                        ),
                    ),
                    TemplateChapter(
                        name = "Matrices & Determinants",
                        topics = listOf(
                            "Types of Matrices & Operations",
                            "Determinants & Properties",
                            "Adjoint & Inverse of a Matrix",
                            "System of Linear Equations - Cramer's Rule",
                        ),
                    ),
                    TemplateChapter(
                        name = "Permutations & Combinations",
                        topics = listOf(
                            "Fundamental Counting Principle",
                            "Permutations - With & Without Repetition",
                            "Combinations & Binomial Coefficients",
                            "Circular Permutations",
                        ),
                    ),
                    TemplateChapter(
                        name = "Binomial Theorem",
                        topics = listOf(
                            "Binomial Expansion & General Term",
                            "Middle Term & Properties of Coefficients",
                            "Multinomial Theorem",
                        ),
                    ),
                    TemplateChapter(
                        name = "Sequences & Series",
                        topics = listOf(
                            "Arithmetic Progression",
                            "Geometric Progression",
                            "Harmonic Progression & AGP",
                            "Sum of Special Series",
                            "Telescoping & Method of Differences",
                        ),
                    ),
                    TemplateChapter(
                        name = "Limits, Continuity & Differentiability",
                        topics = listOf(
                            "Limits - L'Hopital's Rule & Standard Forms",
                            "Continuity & Types of Discontinuity",
                            "Differentiability & Derivatives",
                            "Mean Value Theorem - Rolle's & Lagrange's",
                        ),
                    ),
                    TemplateChapter(
                        name = "Differential Calculus - Applications",
                        topics = listOf(
                            "Tangents & Normals",
                            "Increasing & Decreasing Functions",
                            "Maxima & Minima",
                            "Rate of Change & Approximations",
                        ),
                    ),
                    TemplateChapter(
                        name = "Integral Calculus",
                        topics = listOf(
                            "Indefinite Integrals - Methods",
                            "Definite Integrals - Properties",
                            "Area Under Curves",
                            "Integration by Parts & Partial Fractions",
                            "Reduction Formulas",
                        ),
                    ),
                    TemplateChapter(
                        name = "Differential Equations",
                        topics = listOf(
                            "Order & Degree of DE",
                            "Variable Separable & Homogeneous DE",
                            "Linear Differential Equations",
                            "Applications of DE",
                        ),
                    ),
                    TemplateChapter(
                        name = "Coordinate Geometry",
                        topics = listOf(
                            "Straight Lines - Slope, Distance, Section Formula",
                            "Circles - Equation, Tangent, Normal",
                            "Parabola - Standard Forms & Properties",
                            "Ellipse - Eccentricity & Tangents",
                            "Hyperbola - Asymptotes & Properties",
                        ),
                    ),
                    TemplateChapter(
                        name = "Trigonometry",
                        topics = listOf(
                            "Trigonometric Ratios & Identities",
                            "Trigonometric Equations",
                            "Inverse Trigonometric Functions",
                            "Properties of Triangles - Sine & Cosine Rules",
                            "Heights & Distances",
                        ),
                    ),
                    TemplateChapter(
                        name = "Vector Algebra",
                        topics = listOf(
                            "Types of Vectors & Operations",
                            "Dot Product & Cross Product",
                            "Scalar Triple Product",
                            "Vector Applications in Geometry",
                        ),
                    ),
                    TemplateChapter(
                        name = "3D Geometry",
                        topics = listOf(
                            "Direction Cosines & Ratios",
                            "Equation of a Line in 3D",
                            "Equation of a Plane",
                            "Distance Between Lines & Planes",
                            "Angle Between Line & Plane",
                        ),
                    ),
                    TemplateChapter(
                        name = "Probability & Statistics",
                        topics = listOf(
                            "Conditional Probability & Bayes' Theorem",
                            "Random Variables & Distributions",
                            "Mean, Variance & Standard Deviation",
                            "Binomial Distribution",
                        ),
                    ),
                    TemplateChapter(
                        name = "Mathematical Reasoning & Miscellaneous",
                        topics = listOf(
                            "Statements & Logical Connectives",
                            "Mathematical Induction",
                            "Linear Programming",
                        ),
                    ),
                ),
            ),
        ),
    ),
    ExamTemplate(
        id = "neet-ug",
        name = "NEET UG",
        description = "National Eligibility cum Entrance Test (UG) - 200 questions across Physics, Chemistry, and Biology. 720 marks, 3 hours 20 minutes.",
        recommendedDailyGoal = 4,
        subjects = listOf(
            TemplateSubject(
                name = "Physics",
                color = "#3b82f6",
                chapters = listOf(
                    TemplateChapter(
                        name = "Mechanics",
                        topics = listOf(
                            "Units & Measurements",
                            "Kinematics - Motion in 1D & 2D",
                            "Newton's Laws of Motion & Friction",
                            "Work, Energy & Power",
                            "Rotational Motion & Moment of Inertia",
                            "Gravitation & Satellites",
                        ),
                    ),
                    TemplateChapter(
                        name = "Properties of Matter",
                        topics = listOf(
                            "Elasticity & Stress-Strain",
                            "Fluid Mechanics - Pressure & Bernoulli",
                            "Viscosity & Surface Tension",
                            "Thermal Expansion",
                        ),
                    ),
                    TemplateChapter(
                        name = "Thermodynamics",
                        topics = listOf(
                            "Laws of Thermodynamics",
                            "Kinetic Theory of Gases",
                            "Heat Transfer - Conduction, Convection, Radiation",
                            "Calorimetry & Specific Heat",
                        ),
                    ),
                    TemplateChapter(
                        name = "Oscillations & Waves",
                        topics = listOf(
                            "Simple Harmonic Motion",
                            "Wave Motion - Speed & Superposition",
                            "Standing Waves & Resonance",
                            "Doppler Effect & Sound",
                        ),
                    ),
                    TemplateChapter(
                        name = "Electrostatics",
                        topics = listOf(
                            "Coulomb's Law & Electric Field",
                            "Gauss's Law",
                            "Electric Potential & Capacitance",
                            "Dielectrics",
                        ),
                    ),
                    TemplateChapter(
                        name = "Current Electricity",
                        topics = listOf(
                            "Ohm's Law & Circuits",
                            "Kirchhoff's Laws",
                            "Heating Effect of Current",
                            "Wheatstone Bridge & Potentiometer",
                        ),
                    ),
                    TemplateChapter(
                        name = "Magnetic Effects & EMI",
                        topics = listOf(
                            "Biot-Savart & Ampere's Law",
                            "Force on Current-Carrying Conductor",
                            "Electromagnetic Induction - Faraday's Law",
                            "AC Circuits & Transformers",
                        ),
                    ),
                    TemplateChapter(
                        name = "Optics",
                        topics = listOf(
                            "Ray Optics - Reflection & Refraction",
                            "Lenses & Mirrors - Image Formation",
                            "Wave Optics - Interference & Diffraction",
                            "Optical Instruments",
                        ),
                    ),
                    TemplateChapter(
                        name = "Modern Physics",
                        topics = listOf(
                            "Photoelectric Effect & Dual Nature",
                            "Atomic Models - Bohr's Theory",
                            "Nuclear Physics - Fission & Fusion",
                            "Radioactivity & Decay",
                            "Semiconductor Devices",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "Chemistry",
                color = "#10b981",
                chapters = listOf(
                    TemplateChapter(
                        name = "Atomic Structure & Chemical Bonding",
                        topics = listOf(
                            "Quantum Numbers & Electronic Configuration",
                            "Ionic & Covalent Bonding",
                            "VSEPR Theory & Hybridization",
                            "Molecular Orbital Theory",
                        ),
                    ),
                    TemplateChapter(
                        name = "States of Matter & Solutions",
                        topics = listOf(
                            "Gas Laws - Ideal & Real Gases",
                            "Solid State - Crystal Lattice & Defects",
                            "Solutions - Colligative Properties & Raoult's Law",
                            "Osmotic Pressure",
                        ),
                    ),
                    TemplateChapter(
                        name = "Thermodynamics & Equilibrium",
                        topics = listOf(
                            "Enthalpy, Entropy & Gibbs Energy",
                            "Hess's Law & Born-Haber Cycle",
                            "Chemical Equilibrium - Le Chatelier",
                            "Ionic Equilibrium - pH, Buffers, Solubility Product",
                        ),
                    ),
                    TemplateChapter(
                        name = "Redox & Electrochemistry",
                        topics = listOf(
                            "Redox Reactions & Balancing",
                            "Electrochemical Cells & Nernst Equation",
                            "Electrolysis & Faraday's Laws",
                            "Conductance",
                        ),
                    ),
                    TemplateChapter(
                        name = "Chemical Kinetics & Surface Chemistry",
                        topics = listOf(
                            "Rate of Reaction & Rate Law",
                            "Arrhenius Equation",
                            "Catalysis - Homogeneous & Heterogeneous",
                            "Colloids & Adsorption",
                        ),
                    ),
                    TemplateChapter(
                        name = "Periodic Table & Inorganic Chemistry",
                        topics = listOf(
                            "Periodic Trends - IE, EA, Electronegativity",
                            "s-Block Elements - Alkali & Alkaline Earth",
                            "p-Block Elements - Groups 13 to 18",
                            "d-Block & f-Block Elements",
                            "Coordination Compounds & CFSE",
                        ),
                    ),
                    TemplateChapter(
                        name = "Metallurgy & Environmental Chemistry",
                        topics = listOf(
                            "General Principles of Metallurgy",
                            "Environmental Chemistry - Pollution & Ozone",
                        ),
                    ),
                    TemplateChapter(
                        name = "Organic Chemistry - Basics & Hydrocarbons",
                        topics = listOf(
                            "IUPAC Nomenclature & Isomerism",
                            "Electronic Effects - Inductive, Resonance",
                            "Alkanes, Alkenes & Alkynes",
                            "Aromatic Hydrocarbons - Benzene",
                        ),
                    ),
                    TemplateChapter(
                        name = "Organic Functional Groups",
                        topics = listOf(
                            "Haloalkanes & Haloarenes",
                            "Alcohols, Phenols & Ethers",
                            "Aldehydes & Ketones",
                            "Carboxylic Acids",
                            "Amines & Diazonium Salts",
                        ),
                    ),
                    TemplateChapter(
                        name = "Biomolecules & Polymers",
                        topics = listOf(
                            "Carbohydrates & Amino Acids",
                            "Proteins & Nucleic Acids",
                            "Polymers - Natural & Synthetic",
                            "Chemistry in Everyday Life - Drugs & Detergents",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "Biology (Botany)",
                color = "#8b5cf6",
                chapters = listOf(
                    TemplateChapter(
                        name = "Cell Biology",
                        topics = listOf(
                            "Cell Structure - Prokaryotic & Eukaryotic",
                            "Cell Organelles & Functions",
                            "Cell Division - Mitosis & Meiosis",
                            "Biomolecules - Carbohydrates, Proteins, Lipids, Nucleic Acids",
                        ),
                    ),
                    TemplateChapter(
                        name = "Plant Anatomy & Morphology",
                        topics = listOf(
                            "Root, Stem & Leaf Anatomy",
                            "Flower, Fruit & Seed Morphology",
                            "Tissue Systems - Meristematic & Permanent",
                            "Secondary Growth",
                        ),
                    ),
                    TemplateChapter(
                        name = "Plant Physiology",
                        topics = listOf(
                            "Photosynthesis - Light & Dark Reactions",
                            "Respiration - Glycolysis, Krebs, ETC",
                            "Plant Growth & Development - Hormones",
                            "Mineral Nutrition & Transport in Plants",
                            "Water Relations - Osmosis, Transpiration",
                        ),
                    ),
                    TemplateChapter(
                        name = "Plant Reproduction",
                        topics = listOf(
                            "Sexual Reproduction in Flowering Plants",
                            "Pollination & Fertilization",
                            "Seed & Fruit Development",
                            "Asexual Reproduction - Vegetative Propagation",
                        ),
                    ),
                    TemplateChapter(
                        name = "Genetics & Molecular Biology",
                        topics = listOf(
                            "Mendelian Genetics - Laws of Inheritance",
                            "Chromosomal Theory & Linkage",
                            "DNA Replication & Transcription",
                            "Translation & Gene Expression",
                            "Genetic Code & Mutations",
                        ),
                    ),
                    TemplateChapter(
                        name = "Biotechnology",
                        topics = listOf(
                            "Recombinant DNA Technology",
                            "PCR, Gel Electrophoresis & Cloning",
                            "Applications - GMOs, Gene Therapy",
                            "Bioethics & Biosafety",
                        ),
                    ),
                    TemplateChapter(
                        name = "Ecology & Environment",
                        topics = listOf(
                            "Ecosystem - Structure & Function",
                            "Energy Flow & Nutrient Cycling",
                            "Ecological Succession",
                            "Biodiversity & Conservation",
                            "Environmental Issues - Pollution & Climate Change",
                        ),
                    ),
                ),
            ),
            TemplateSubject(
                name = "Biology (Zoology)",
                color = "#ef4444",
                chapters = listOf(
                    TemplateChapter(
                        name = "Animal Diversity & Classification",
                        topics = listOf(
                            "Basis of Classification - Phyla Overview",
                            "Non-Chordates - Porifera to Echinodermata",
                            "Chordates - Pisces to Mammalia",
                            "Structural Organization in Animals",
                        ),
                    ),
                    TemplateChapter(
                        name = "Human Physiology",
                        topics = listOf(
                            "Digestion & Absorption",
                            "Breathing & Gas Exchange",
                            "Circulation - Heart, Blood, Blood Groups",
                            "Excretion - Kidney & Urine Formation",
                            "Nervous System & Neural Control",
                            "Endocrine System & Hormones",
                            "Locomotion & Movement - Muscles & Skeleton",
                        ),
                    ),
                    TemplateChapter(
                        name = "Human Reproduction & Development",
                        topics = listOf(
                            "Male & Female Reproductive Systems",
                            "Gametogenesis & Fertilization",
                            "Embryonic Development & Pregnancy",
                            "Reproductive Health & Contraception",
                        ),
                    ),
                    TemplateChapter(
                        name = "Evolution",
                        topics = listOf(
                            "Origin of Life - Chemical Evolution",
                            "Darwinism & Natural Selection",
                            "Hardy-Weinberg Principle",
                            "Human Evolution",
                        ),
                    ),
                    TemplateChapter(
                        name = "Human Health & Disease",
                        topics = listOf(
                            "Common Diseases - Bacterial, Viral, Parasitic",
                            "Immunity - Innate & Adaptive",
                            "AIDS, Cancer & Drug Abuse",
                            "Vaccines & Immunization",
                        ),
                    ),
                    TemplateChapter(
                        name = "Microbes & Applications",
                        topics = listOf(
                            "Microbes in Household & Industry",
                            "Microbes in Sewage Treatment",
                            "Microbes as Biocontrol Agents",
                            "Microbes in Biogas & Biofertilizers",
                        ),
                    ),
                ),
            ),
        ),
    ),
)
