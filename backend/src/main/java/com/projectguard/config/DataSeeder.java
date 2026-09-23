package com.projectguard.config;

import com.projectguard.entity.*;
import com.projectguard.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final EngineeringBranchRepository branchRepository;
    private final ProjectDomainRepository domainRepository;
    private final SkillRepository skillRepository;
    private final TechnologyRepository technologyRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DataSeeder(EngineeringBranchRepository branchRepository,
                      ProjectDomainRepository domainRepository,
                      SkillRepository skillRepository,
                      TechnologyRepository technologyRepository,
                      AssessmentQuestionRepository questionRepository,
                      org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.branchRepository = branchRepository;
        this.domainRepository = domainRepository;
        this.skillRepository = skillRepository;
        this.technologyRepository = technologyRepository;
        this.questionRepository = questionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE technologies DROP INDEX uk_technologies_name");
        } catch (Exception e) {}
        try {
            jdbcTemplate.execute("ALTER TABLE technologies DROP INDEX name");
        } catch (Exception e) {}
        try {
            jdbcTemplate.execute("ALTER TABLE technologies DROP KEY name");
        } catch (Exception e) {}

        // ─── BRANCHES ─────────────────────────────────────────────────────────
       EngineeringBranch cse  = branch("Computer Science and Engineering", "Computer Science and Engineering");
EngineeringBranch ise  = branch("Information Science and Engineering", "Information Science and Engineering");
EngineeringBranch ece  = branch("Electronics and Communication Engineering", "Electronics and Communication Engineering");
EngineeringBranch eee  = branch("Electrical and Electronics Engineering", "Electrical and Electronics Engineering");
EngineeringBranch mech = branch("Mechanical Engineering", "Mechanical Engineering");
EngineeringBranch civil = branch("Civil Engineering", "Civil Engineering");

        // ─── SKILLS (global, de-duplicated by name) ────────────────────────────
        // CSE/ISE
        Skill python      = skill("Python");
        Skill java        = skill("Java");
        Skill numpy       = skill("NumPy");
        Skill pandas      = skill("Pandas");
        Skill statistics  = skill("Statistics");
        Skill ml          = skill("Machine Learning");
        Skill dl          = skill("Deep Learning");
        Skill sklearn     = skill("Scikit-learn");
        Skill tfpt        = skill("TensorFlow/PyTorch");
        Skill spring      = skill("Spring");
        Skill springBoot  = skill("Spring Boot");
        Skill restApi     = skill("REST API");
        Skill jpaHib      = skill("JPA/Hibernate");
        Skill react       = skill("React");
        Skill sqlMysql    = skill("SQL/MySQL");
        Skill git         = skill("Git");
        Skill maven       = skill("Maven");
        Skill djangoFlask = skill("Django/Flask");
        Skill sql         = skill("SQL");
        Skill javascript  = skill("JavaScript");
        Skill nodejs      = skill("Node.js");
        Skill expressjs   = skill("Express.js");
        Skill mongodb     = skill("MongoDB");
        Skill csharp      = skill("C#");
        Skill aspnet      = skill("ASP.NET Core");
        Skill webApi      = skill("Web API");
        Skill sqlServer   = skill("SQL Server");
        Skill php         = skill("PHP");
        Skill laravel     = skill("Laravel");
        Skill mysql       = skill("MySQL");
        Skill javaKotlin  = skill("Java/Kotlin");
        Skill android     = skill("Android Development");
        Skill flutter     = skill("Flutter");
        Skill dart        = skill("Dart");
        Skill reactNative = skill("React Native");
        Skill firebase    = skill("Firebase");
        Skill dataViz     = skill("Data Visualization");
        Skill powerBI     = skill("Power BI");
        Skill networking  = skill("Networking");
        Skill linux       = skill("Linux");
        Skill crypto      = skill("Cryptography");
        Skill ethHack     = skill("Ethical Hacking");
        Skill webSec      = skill("Web Security");
        Skill netSec      = skill("Network Security");
        Skill digForensics= skill("Digital Forensics");
        Skill aws         = skill("AWS");
        Skill azure       = skill("Azure");
        Skill docker      = skill("Docker");
        Skill k8s         = skill("Kubernetes");
        Skill cloudSec    = skill("Cloud Security");

        // ECE/EEE
        Skill cProg       = skill("C");
        Skill embeddedC   = skill("Embedded C");
        Skill mcu         = skill("Microcontrollers");
        Skill mpu         = skill("Microprocessors");
        Skill sensors     = skill("Sensors");
        Skill uartSpiI2c  = skill("UART/SPI/I2C");
        Skill rtos        = skill("RTOS");
        Skill arduino     = skill("Arduino");
        Skill esp32       = skill("ESP32");
        Skill raspberryPi = skill("Raspberry Pi");
        Skill mqtt        = skill("MQTT");
        Skill iotProto    = skill("IoT Protocols");
        Skill digElec     = skill("Digital Electronics");
        Skill boolLogic   = skill("Boolean Logic");
        Skill verilog     = skill("Verilog");
        Skill vhdl        = skill("VHDL");
        Skill fpga        = skill("FPGA");
        Skill cmos        = skill("CMOS");
        Skill rtlDesign   = skill("RTL Design");
        Skill cpp         = skill("C/C++");
        Skill motors      = skill("Motors & Actuators");
        Skill ctrlSystems = skill("Control Systems");
        Skill ros         = skill("ROS");
        Skill analogComm  = skill("Analog Communication");
        Skill digComm     = skill("Digital Communication");
        Skill sigProc     = skill("Signal Processing");
        Skill modulation  = skill("Modulation");
        Skill antennas    = skill("Antennas");
        Skill wirelessComm= skill("Wireless Communication");
        Skill matlab      = skill("MATLAB");

        // EEE
        Skill elecCircuits= skill("Electrical Circuits");
        Skill powerSystems= skill("Power Systems");
        Skill transformers = skill("Transformers");
        Skill generators   = skill("Generators");
        Skill transDistrib = skill("Transmission & Distribution");
        Skill powerProtect = skill("Power System Protection");
        Skill matlabSimulink= skill("MATLAB/Simulink");
        Skill solarEnergy  = skill("Solar Energy");
        Skill windEnergy   = skill("Wind Energy");
        Skill solarPV      = skill("Solar PV");
        Skill powerElec    = skill("Power Electronics");
        Skill energyStorage= skill("Energy Storage");
        Skill batterySys   = skill("Battery Systems");
        Skill smartGrid    = skill("Smart Grid");
        Skill commProtos   = skill("Communication Protocols");
        Skill actuators    = skill("Actuators");
        Skill plc          = skill("PLC");
        Skill elecMotors   = skill("Electric Motors");
        Skill drives       = skill("Drives");
        Skill indAuto      = skill("Industrial Automation");

        // Mechanical
        Skill engDrawing   = skill("Engineering Drawing");
        Skill autoCAD      = skill("AutoCAD");
        Skill solidWorks   = skill("SolidWorks");
        Skill catia        = skill("CATIA");
        Skill modeling3d   = skill("3D Modeling");
        Skill gdt          = skill("GD&T");
        Skill productDesign= skill("Product Design");
        Skill mfgProcesses = skill("Manufacturing Processes");
        Skill cnc          = skill("CNC");
        Skill cam          = skill("CAM");
        Skill printing3d   = skill("3D Printing");
        Skill prodPlanning = skill("Production Planning");
        Skill qualControl  = skill("Quality Control");
        Skill robotics     = skill("Robotics");
        Skill autoSystems  = skill("Automotive Systems");
        Skill icEngines    = skill("IC Engines");
        Skill vehicleDyn   = skill("Vehicle Dynamics");
        Skill autoElec     = skill("Automobile Electronics");
        Skill evTech       = skill("EV Technology");
        Skill thermoDyn    = skill("Thermodynamics");
        Skill heatTransfer = skill("Heat Transfer");
        Skill fluidMech    = skill("Fluid Mechanics");
        Skill refrigeration= skill("Refrigeration");
        Skill hvac         = skill("HVAC");
        Skill mechSystems  = skill("Mechanical Systems");

        // Civil
        Skill structAnalysis= skill("Structural Analysis");
        Skill strengthMat  = skill("Strength of Materials");
        Skill rccDesign    = skill("RCC Design");
        Skill steelDesign  = skill("Steel Design");
        Skill staadPro     = skill("STAAD.Pro");
        Skill etabs        = skill("ETABS");
        Skill constrMethods= skill("Construction Methods");
        Skill bldgMaterials= skill("Building Materials");
        Skill estimation   = skill("Estimation & Costing");
        Skill projPlanning = skill("Project Planning");
        Skill constrMgmt   = skill("Construction Management");
        Skill qtySurveying = skill("Quantity Surveying");
        Skill soilMech     = skill("Soil Mechanics");
        Skill foundEng     = skill("Foundation Engineering");
        Skill soilTesting  = skill("Soil Testing");
        Skill geoInvest    = skill("Geotechnical Investigation");
        Skill bearingCap   = skill("Bearing Capacity");
        Skill slopeStab    = skill("Slope Stability");
        Skill plaxis       = skill("PLAXIS");
        Skill highwayEng   = skill("Highway Engineering");
        Skill trafficEng   = skill("Traffic Engineering");
        Skill pavementDesign= skill("Pavement Design");
        Skill transpPlan   = skill("Transportation Planning");
        Skill roadSafety   = skill("Road Safety");
        Skill gis          = skill("GIS");
        Skill waterTreat   = skill("Water Treatment");
        Skill wastewaterTreat= skill("Wastewater Treatment");
        Skill solidWaste   = skill("Solid Waste Management");
        Skill airPollution = skill("Air Pollution");
        Skill eia          = skill("Environmental Impact Assessment");
        Skill waterQuality = skill("Water Quality");
        Skill envMonitor   = skill("Environmental Monitoring");
        Skill hydrology    = skill("Hydrology");
        Skill irrigationEng= skill("Irrigation Engineering");
        Skill hydraulicStr = skill("Hydraulic Structures");
        Skill groundwater  = skill("Groundwater");
        Skill floodMgmt    = skill("Flood Management");
        Skill waterResPlan = skill("Water Resource Planning");

                // ─── CSE + ISE DOMAINS ─────────────────────────────────────────────────
        ProjectDomain aiMl    = domain("AI / Machine Learning");
        ProjectDomain webDev  = domain("Web Development");
        ProjectDomain mobileApp= domain("Mobile App Development");
        ProjectDomain dataSci = domain("Data Science & Analytics");
        ProjectDomain cyberSec= domain("Cyber Security");
        ProjectDomain cloudComp= domain("Cloud Computing");

        link(cse, aiMl, webDev, mobileApp, dataSci, cyberSec, cloudComp);
        link(ise, aiMl, webDev, mobileApp, dataSci, cyberSec, cloudComp);

        tech("Python ML",          aiMl, python, numpy, pandas, statistics, ml, dl, sklearn, tfpt);
        tech("Scikit-learn",       aiMl, python, numpy, pandas, statistics, ml, dl, sklearn, tfpt);
        tech("TensorFlow/PyTorch", aiMl, python, numpy, pandas, statistics, ml, dl, sklearn, tfpt);

        tech("Java Full Stack",    webDev, java, spring, springBoot, restApi, jpaHib, react, sqlMysql, git, maven);
        tech("Python Full Stack",  webDev, python, djangoFlask, restApi, react, sql, git);
        tech("MERN Full Stack",    webDev, javascript, react, nodejs, expressjs, mongodb, restApi, git);
        tech(".NET Full Stack",    webDev, csharp, aspnet, webApi, sqlServer, react, git);
        tech("PHP Full Stack",     webDev, php, laravel, mysql, javascript, restApi, git);

        tech("Android",      mobileApp, javaKotlin, android, flutter, dart, reactNative, restApi, firebase);
        tech("Flutter",      mobileApp, javaKotlin, android, flutter, dart, reactNative, restApi, firebase);
        tech("React Native", mobileApp, javaKotlin, android, flutter, dart, reactNative, restApi, firebase);

        tech("Python Data Stack", dataSci, python, statistics, pandas, numpy, sql, dataViz, powerBI, ml);
        tech("BI/Analytics",      dataSci, python, statistics, pandas, numpy, sql, dataViz, powerBI, ml);

        tech("Network Security",  cyberSec, networking, linux, crypto, ethHack, webSec, netSec, digForensics);
        tech("Web Security",      cyberSec, networking, linux, crypto, ethHack, webSec, netSec, digForensics);
        tech("Ethical Hacking",   cyberSec, networking, linux, crypto, ethHack, webSec, netSec, digForensics);

        tech("AWS",        cloudComp, linux, networking, aws, azure, docker, k8s, cloudSec);
        tech("Azure",      cloudComp, linux, networking, aws, azure, docker, k8s, cloudSec);
        tech("Containers", cloudComp, linux, networking, aws, azure, docker, k8s, cloudSec);

        // ─── ECE DOMAINS ───────────────────────────────────────────────────────
        ProjectDomain embSysEce  = domain("Embedded Systems");
        ProjectDomain iotEce     = domain("IoT");
        ProjectDomain vlsi       = domain("VLSI / Digital Design");
        ProjectDomain roboticsEce= domain("Robotics & Automation");
        ProjectDomain commSys    = domain("Communication Systems");

        link(ece, embSysEce, iotEce, vlsi, roboticsEce, commSys);

        tech("Embedded C",       embSysEce, cProg, embeddedC, mcu, mpu, sensors, uartSpiI2c, rtos);
        tech("Microcontrollers", embSysEce, cProg, embeddedC, mcu, mpu, sensors, uartSpiI2c, rtos);
        tech("RTOS",             embSysEce, cProg, embeddedC, mcu, mpu, sensors, uartSpiI2c, rtos);

        tech("Arduino",          iotEce, embeddedC, sensors, arduino, esp32, raspberryPi, mqtt, iotProto);
        tech("ESP32",            iotEce, embeddedC, sensors, arduino, esp32, raspberryPi, mqtt, iotProto);
        tech("Raspberry Pi",     iotEce, embeddedC, sensors, arduino, esp32, raspberryPi, mqtt, iotProto);

        tech("Verilog",          vlsi, digElec, boolLogic, verilog, vhdl, fpga, cmos, rtlDesign);
        tech("VHDL",             vlsi, digElec, boolLogic, verilog, vhdl, fpga, cmos, rtlDesign);
        tech("FPGA",             vlsi, digElec, boolLogic, verilog, vhdl, fpga, cmos, rtlDesign);

        tech("Robotics",         roboticsEce, cpp, sensors, mcu, arduino, motors, ctrlSystems, ros);
        tech("ROS",              roboticsEce, cpp, sensors, mcu, arduino, motors, ctrlSystems, ros);
        tech("Microcontrollers_ECE", roboticsEce, cpp, sensors, mcu, arduino, motors, ctrlSystems, ros);

        tech("Communication Systems", commSys, analogComm, digComm, sigProc, modulation, antennas, wirelessComm, matlab);
        tech("MATLAB",           commSys, analogComm, digComm, sigProc, modulation, antennas, wirelessComm, matlab);

        // ─── EEE DOMAINS ───────────────────────────────────────────────────────
        ProjectDomain powerSysEee = domain("Electrical Power Systems");
        ProjectDomain renewEnergy = domain("Renewable Energy");
        ProjectDomain embSysEee   = domain("Embedded Systems");
        ProjectDomain iotEee      = domain("IoT");
        ProjectDomain roboticsEee = domain("Robotics & Automation");
        ProjectDomain elecDrives  = domain("Electrical Drives & Control");

        link(eee, powerSysEee, renewEnergy, embSysEee, iotEee, roboticsEee, elecDrives);

        tech("Power Systems",      powerSysEee, elecCircuits, powerSystems, transformers, generators, transDistrib, powerProtect, matlabSimulink);
        tech("MATLAB/Simulink",    powerSysEee, elecCircuits, powerSystems, transformers, generators, transDistrib, powerProtect, matlabSimulink);

        tech("Solar",              renewEnergy, solarEnergy, windEnergy, solarPV, powerElec, energyStorage, batterySys, smartGrid);
        tech("Wind",               renewEnergy, solarEnergy, windEnergy, solarPV, powerElec, energyStorage, batterySys, smartGrid);
        tech("Smart Grid",         renewEnergy, solarEnergy, windEnergy, solarPV, powerElec, energyStorage, batterySys, smartGrid);

        tech("Embedded C_EEE",         embSysEee, cProg, embeddedC, mcu, sensors, arduino, esp32, commProtos);
        tech("Microcontrollers_EEE",   embSysEee, cProg, embeddedC, mcu, sensors, arduino, esp32, commProtos);

        tech("Arduino_EEE",            iotEee, sensors, arduino, esp32, raspberryPi, mqtt, embeddedC, iotProto);
        tech("ESP32_EEE",              iotEee, sensors, arduino, esp32, raspberryPi, mqtt, embeddedC, iotProto);
        tech("Raspberry Pi_EEE",       iotEee, sensors, arduino, esp32, raspberryPi, mqtt, embeddedC, iotProto);

        tech("PLC_EEE",                roboticsEee, ctrlSystems, sensors, motors, actuators, plc, robotics, matlabSimulink);
        tech("Robotics_EEE",           roboticsEee, ctrlSystems, sensors, motors, actuators, plc, robotics, matlabSimulink);
        tech("Control Systems",    roboticsEee, ctrlSystems, sensors, motors, actuators, plc, robotics, matlabSimulink);

        tech("Electrical Drives",  elecDrives, ctrlSystems, powerElec, elecMotors, drives, plc, matlabSimulink, indAuto);
        tech("PLC_Drives",                elecDrives, ctrlSystems, powerElec, elecMotors, drives, plc, matlabSimulink, indAuto);
        tech("Industrial Automation", elecDrives, ctrlSystems, powerElec, elecMotors, drives, plc, matlabSimulink, indAuto);

        // ─── MECHANICAL DOMAINS ────────────────────────────────────────────────
        ProjectDomain cad          = domain("CAD / Product Design");
        ProjectDomain manufacturing= domain("Manufacturing");
        ProjectDomain roboticsMech = domain("Robotics & Automation");
        ProjectDomain automotive   = domain("Automotive Engineering");
        ProjectDomain thermal      = domain("Thermal Engineering");
        ProjectDomain mechatronics = domain("Mechatronics");

        link(mech, cad, manufacturing, roboticsMech, automotive, thermal, mechatronics);

        tech("AutoCAD",     cad, engDrawing, autoCAD, solidWorks, catia, modeling3d, gdt, productDesign);
        tech("SolidWorks",  cad, engDrawing, autoCAD, solidWorks, catia, modeling3d, gdt, productDesign);
        tech("CATIA",       cad, engDrawing, autoCAD, solidWorks, catia, modeling3d, gdt, productDesign);

        tech("CNC",         manufacturing, mfgProcesses, cnc, cam, printing3d, prodPlanning, qualControl, indAuto);
        tech("CAM",         manufacturing, mfgProcesses, cnc, cam, printing3d, prodPlanning, qualControl, indAuto);
        tech("3D Printing", manufacturing, mfgProcesses, cnc, cam, printing3d, prodPlanning, qualControl, indAuto);

        tech("Robotics_Mech",    roboticsMech, robotics, sensors, actuators, plc, ctrlSystems, matlab, indAuto);
        tech("PLC_Mech",         roboticsMech, robotics, sensors, actuators, plc, ctrlSystems, matlab, indAuto);
        tech("MATLAB_Mech",      roboticsMech, robotics, sensors, actuators, plc, ctrlSystems, matlab, indAuto);

        tech("Automotive Systems", automotive, autoSystems, icEngines, vehicleDyn, autoElec, autoCAD, mfgProcesses, evTech);
        tech("EV",          automotive, autoSystems, icEngines, vehicleDyn, autoElec, autoCAD, mfgProcesses, evTech);

        tech("Thermal Systems", thermal, thermoDyn, heatTransfer, fluidMech, icEngines, refrigeration, hvac, matlabSimulink);
        tech("HVAC",        thermal, thermoDyn, heatTransfer, fluidMech, icEngines, refrigeration, hvac, matlabSimulink);
        tech("MATLAB/Simulink_Thermal", thermal, thermoDyn, heatTransfer, fluidMech, icEngines, refrigeration, hvac, matlabSimulink);

        tech("Mechatronics", mechatronics, mechSystems, sensors, actuators, mcu, plc, ctrlSystems, robotics);
        tech("PLC_Mechatronics",          mechatronics, mechSystems, sensors, actuators, mcu, plc, ctrlSystems, robotics);
        tech("Robotics_Mechatronics",     mechatronics, mechSystems, sensors, actuators, mcu, plc, ctrlSystems, robotics);

        // ─── CIVIL DOMAINS ─────────────────────────────────────────────────────
        ProjectDomain structural   = domain("Structural Engineering");
        ProjectDomain construction = domain("Construction Technology");
        ProjectDomain geotech      = domain("Geotechnical Engineering");
        ProjectDomain transport    = domain("Transportation Engineering");
        ProjectDomain environmental= domain("Environmental Engineering");
        ProjectDomain waterRes     = domain("Water Resources Engineering");

        link(civil, structural, construction, geotech, transport, environmental, waterRes);

        tech("AutoCAD_Struct",          structural, structAnalysis, strengthMat, rccDesign, steelDesign, autoCAD, staadPro, etabs);
        tech("STAAD.Pro",        structural, structAnalysis, strengthMat, rccDesign, steelDesign, autoCAD, staadPro, etabs);
        tech("ETABS",            structural, structAnalysis, strengthMat, rccDesign, steelDesign, autoCAD, staadPro, etabs);

        tech("Construction Management", construction, constrMethods, bldgMaterials, estimation, projPlanning, constrMgmt, autoCAD, qtySurveying);
        tech("AutoCAD_Const",                 construction, constrMethods, bldgMaterials, estimation, projPlanning, constrMgmt, autoCAD, qtySurveying);

        tech("Geotechnical Analysis", geotech, soilMech, foundEng, soilTesting, geoInvest, bearingCap, slopeStab, plaxis);
        tech("PLAXIS",                geotech, soilMech, foundEng, soilTesting, geoInvest, bearingCap, slopeStab, plaxis);

        tech("Transportation Planning", transport, highwayEng, trafficEng, pavementDesign, transpPlan, roadSafety, autoCAD, gis);
        tech("GIS",                     transport, highwayEng, trafficEng, pavementDesign, transpPlan, roadSafety, autoCAD, gis);
        tech("AutoCAD_Transp",                 transport, highwayEng, trafficEng, pavementDesign, transpPlan, roadSafety, autoCAD, gis);

        tech("Water Treatment",          environmental, waterTreat, wastewaterTreat, solidWaste, airPollution, eia, waterQuality, envMonitor);
        tech("Environmental Monitoring", environmental, waterTreat, wastewaterTreat, solidWaste, airPollution, eia, waterQuality, envMonitor);

        tech("Hydrology",        waterRes, hydrology, fluidMech, irrigationEng, hydraulicStr, groundwater, floodMgmt, waterResPlan);
        tech("Hydraulics",       waterRes, hydrology, fluidMech, irrigationEng, hydraulicStr, groundwater, floodMgmt, waterResPlan);

        // ─── ENSURE ALL TECHNOLOGY SKILLS HAVE QUESTIONS ─────────────────────
        ensureQuestionsForAllTechSkills();
    }

    // ─── Idempotent helpers ──────────────────────────────────────────────────

    private EngineeringBranch branch(String name, String desc) {
        return branchRepository.findAll().stream()
                .filter(b -> b.getName().equalsIgnoreCase(name)).findFirst()
                .orElseGet(() -> {
                    EngineeringBranch b = new EngineeringBranch();
                    b.setName(name);
                    b.setDescription(desc);
                    return branchRepository.save(b);
                });
    }

    private ProjectDomain domain(String name) {
        return domainRepository.findAll().stream()
                .filter(d -> d.getName().equalsIgnoreCase(name)).findFirst()
                .orElseGet(() -> {
                    ProjectDomain d = new ProjectDomain();
                    d.setName(name);
                    return domainRepository.save(d);
                });
    }

    private Skill skill(String name) {
        return skillRepository.findByName(name)
                .orElseGet(() -> {
                    Skill s = new Skill();
                    s.setName(name);
                    return skillRepository.save(s);
                });
    }

    private void link(EngineeringBranch branch, ProjectDomain... domains) {
        boolean changed = false;
        for (ProjectDomain d : domains) {
            if (!branch.getDomains().contains(d)) {
                branch.getDomains().add(d);
                changed = true;
            }
        }
        if (changed) branchRepository.save(branch);
    }

    private Technology tech(String name, ProjectDomain domain, Skill... skills) {
        // Find technology by domain and name (unique per domain)
        Technology t = technologyRepository.findByDomainIdAndName(domain.getId(), name)
                .orElseGet(() -> {
                    Technology nt = new Technology();
                    nt.setName(name);
                    nt.setDomain(domain);
                    return technologyRepository.save(nt);
                });

        boolean changed = false;
        for (Skill s : skills) {
            if (!t.getSkills().contains(s)) {
                t.getSkills().add(s);
                changed = true;
            }
        }
        if (changed) technologyRepository.save(t);
        return t;
    }

    // ─── Assessment question generation ──────────────────────────────────────

    private void ensureQuestionsForAllTechSkills() {
        // Gather all skills across all technologies
        List<Technology> allTechs = technologyRepository.findAll();
        for (Technology t : allTechs) {
            for (Skill s : t.getSkills()) {
                ensureQuestions(s);
            }
        }
    }

    private void ensureQuestions(Skill skill) {
        List<AssessmentQuestion> existing = questionRepository.findBySkillId(skill.getId());
        if (existing.size() >= 50) return;

        // Delete any partial seed
        if (!existing.isEmpty()) questionRepository.deleteAll(existing);

        String n = skill.getName();
        
        String[][] easyTemplates = {
            {"What is %s primarily used for?", "Which of the following best describes %s?", "How do you set up a basic environment for %s?"},
            {"In what scenario would %s be most beneficial?", "What is the primary role of %s in a project?", "What are the initial steps to configure %s?"},
            {"Why do developers choose %s for their projects?", "How does %s compare to its basic alternatives?", "What is required to start building with %s?"},
            {"What is a key fundamental concept of %s?", "Which statement accurately defines %s?", "How is a %s workspace initialized?"},
            {"For what specific problem was %s designed?", "What is the industry consensus on %s?", "Where can you find the official setup guide for %s?"}
        };

        String[][] mediumTemplates = {
            {"What is a core feature of %s?", "How does %s handle scalability?", "What design pattern is most commonly associated with %s?", "What is the primary advantage of using %s over alternatives?"},
            {"Which advanced capability defines %s?", "How can %s be scaled horizontally?", "Which architectural pattern suits %s best?", "What makes %s highly efficient in production?"},
            {"What hidden feature of %s is often overlooked?", "How does %s manage high concurrency?", "How does %s enforce code maintainability?", "Why is %s preferred for enterprise applications?"},
            {"What is the lifecycle process in %s?", "How do you balance load in a %s environment?", "Which anti-pattern should be avoided in %s?", "What is the most significant performance benefit of %s?"},
            {"How does %s integrate with external APIs?", "What caching strategies work best with %s?", "How does %s implement modularity?", "What is the standard deployment strategy for %s?"}
        };

        String[][] hardTemplates = {
            {"How would you optimize performance in a large-scale %s project?", "Explain how you would debug a critical failure in %s.", "What is the algorithmic/computational complexity consideration most important in %s?"},
            {"How do you resolve memory leaks in a %s application?", "What is the root cause of common race conditions in %s?", "How do you achieve O(1) time complexity for state retrieval in %s?"},
            {"What is the most effective way to profile a %s system?", "How would you recover a corrupted state in %s?", "How does %s manage garbage collection and memory allocation?"},
            {"How do you implement zero-downtime deployments for %s?", "What tracing techniques isolate bottlenecks in %s?", "How does %s handle eventual consistency in distributed setups?"},
            {"What is the impact of heavy thread contention in %s?", "How do you debug asynchronous deadlocks in %s?", "What are the space complexity trade-offs when caching in %s?"}
        };

        for (int i = 0; i < 5; i++) {
            // EASY: 3 questions per loop = 15 total
            saveQ(skill, String.format(easyTemplates[i][0], n),
                    AssessmentQuestion.Difficulty.EASY, 0,
                    n + " is a fundamental tool/language in its domain.",
                    "Its core domain use-case", "As an art tool", "As a culinary ingredient", "As a transport method");

            saveQ(skill, String.format(easyTemplates[i][1], n),
                    AssessmentQuestion.Difficulty.EASY, 1,
                    n + " is widely recognized as an industry-standard.",
                    "A deprecated legacy tool", "An industry-standard technology", "A social network", "A sports activity");

            saveQ(skill, String.format(easyTemplates[i][2], n),
                    AssessmentQuestion.Difficulty.EASY, 2,
                    "Setting up " + n + " requires installing its environment and dependencies.",
                    "Buy expensive hardware", "Install OS from scratch", "Install " + n + " environment/SDK", "Upload to App Store");

            // MEDIUM: 4 questions per loop = 20 total
            saveQ(skill, String.format(mediumTemplates[i][0], n),
                    AssessmentQuestion.Difficulty.MEDIUM, 0,
                    n + " provides a specific core capability central to its domain.",
                    "Its defining core capability", "Making coffee", "Playing video games", "Painting walls");

            saveQ(skill, String.format(mediumTemplates[i][1], n),
                    AssessmentQuestion.Difficulty.MEDIUM, 1,
                    n + " scales using design patterns appropriate to its domain.",
                    "It does not scale", "Through domain-specific design patterns", "By buying more RAM", "Through manual tuning only");

            saveQ(skill, String.format(mediumTemplates[i][2], n),
                    AssessmentQuestion.Difficulty.MEDIUM, 2,
                    n + " uses standard industry design patterns.",
                    "Anti-pattern", "Spaghetti architecture", "Standard domain design pattern", "No pattern at all");

            saveQ(skill, String.format(mediumTemplates[i][3], n),
                    AssessmentQuestion.Difficulty.MEDIUM, 3,
                    n + " offers efficiency and standardization in its specific domain.",
                    "It is slower", "It is unsupported", "It is expensive", "Efficiency and domain standardization");

            // HARD: 3 questions per loop = 15 total
            saveQ(skill, String.format(hardTemplates[i][0], n),
                    AssessmentQuestion.Difficulty.HARD, 0,
                    "Large-scale " + n + " optimization requires profiling and architecture decisions.",
                    "Profiling and strategic architecture", "Rebooting the server", "Reinstalling the OS", "Ignoring the issue");

            saveQ(skill, String.format(hardTemplates[i][1], n),
                    AssessmentQuestion.Difficulty.HARD, 1,
                    "Debugging " + n + " requires systematic log analysis and root-cause investigation.",
                    "Delete and restart", "Systematic log analysis and root-cause investigation", "Call vendor support immediately", "Blame hardware");

            saveQ(skill, String.format(hardTemplates[i][2], n),
                    AssessmentQuestion.Difficulty.HARD, 2,
                    n + " implementations must account for time and space complexity of core algorithms.",
                    "O(n!) is acceptable", "Complexity is irrelevant", "Time and space complexity of core algorithms", "Only memory matters");
        }
    }

    private void saveQ(Skill skill, String text, AssessmentQuestion.Difficulty diff,
                        int correctIdx, String explanation, String... options) {
        AssessmentQuestion q = new AssessmentQuestion();
        q.setSkill(skill);
        q.setQuestionText(text);
        q.setDifficulty(diff);
        q.setCorrectOptionIndex(correctIdx);
        q.setExplanation(explanation);
        for (String opt : options) {
            AssessmentOption ao = new AssessmentOption(opt);
            q.addOption(ao);
        }
        questionRepository.save(q);
    }
}
