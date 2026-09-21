import re

with open(r'backend\src\main\java\com\projectguard\config\DataSeeder.java', 'r', encoding='utf-8') as f:
    content = f.read()

replacement = """        // ─── CSE + ISE DOMAINS ─────────────────────────────────────────────────
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
        tech("Hydraulics",       waterRes, hydrology, fluidMech, irrigationEng, hydraulicStr, groundwater, floodMgmt, waterResPlan);"""

new_content = re.sub(
    r'// ─── CSE \+ ISE DOMAINS ─────────────────────────────────────────────────.*?// ─── ENSURE ALL TECHNOLOGY SKILLS HAVE QUESTIONS ─────────────────────',
    replacement + '\n\n        // ─── ENSURE ALL TECHNOLOGY SKILLS HAVE QUESTIONS ─────────────────────',
    content,
    flags=re.DOTALL
)

with open(r'backend\src\main\java\com\projectguard\config\DataSeeder.java', 'w', encoding='utf-8') as f:
    f.write(new_content)
