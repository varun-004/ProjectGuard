package com.projectguard.config;

import com.projectguard.entity.*;
import com.projectguard.repository.*;
import com.projectguard.service.ProjectQualityGateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@Order(2)
public class ProjectCatalogSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final ProjectDomainRepository domainRepository;
    private final EngineeringBranchRepository branchRepository;
    private final TechnologyRepository technologyRepository;
    private final SkillRepository skillRepository;
    private final ProjectQualityGateService qualityGateService;

    public ProjectCatalogSeeder(ProjectRepository projectRepository,
                                ProjectDomainRepository domainRepository,
                                EngineeringBranchRepository branchRepository,
                                TechnologyRepository technologyRepository,
                                SkillRepository skillRepository,
                                ProjectQualityGateService qualityGateService) {
        this.projectRepository = projectRepository;
        this.domainRepository = domainRepository;
        this.branchRepository = branchRepository;
        this.technologyRepository = technologyRepository;
        this.skillRepository = skillRepository;
        this.qualityGateService = qualityGateService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Map<String, EngineeringBranch> branches = new HashMap<>();
        for (EngineeringBranch b : branchRepository.findAll()) {
            branches.put(b.getName().toLowerCase(), b);
        }

        Map<String, ProjectDomain> domains = new HashMap<>();
        for (ProjectDomain d : domainRepository.findAll()) {
            domains.put(d.getName().toLowerCase(), d);
        }

        Map<String, Skill> skills = new HashMap<>();
        for (Skill s : skillRepository.findAll()) {
            skills.put(s.getName().toLowerCase(), s);
        }

        Map<String, Technology> technologies = new HashMap<>();
        for (Technology t : technologyRepository.findAll()) {
            technologies.put(t.getName().toLowerCase(), t);
        }

        seedCatalog(branches, domains, skills, technologies);
    }

    private void seedCatalog(Map<String, EngineeringBranch> branches,
                             Map<String, ProjectDomain> domains,
                             Map<String, Skill> skills,
                             Map<String, Technology> technologies) {

        // Convenience branch lists
        List<EngineeringBranch> cseBranchGroup = getBranches(branches,
                "Computer Science and Engineering", "Information Science and Engineering",
                "Bachelor of Computer Applications (BCA)", "Master of Computer Applications (MCA)",
                "B.Sc Computer Science", "M.Sc Computer Science");

        List<EngineeringBranch> eceBranchGroup = getBranches(branches,
                "Electronics and Communication Engineering", "Computer Science and Engineering",
                "Information Science and Engineering");

        List<EngineeringBranch> eeeBranchGroup = getBranches(branches,
                "Electrical and Electronics Engineering", "Electronics and Communication Engineering");

        List<EngineeringBranch> mechBranchGroup = getBranches(branches,
                "Mechanical Engineering");

        List<EngineeringBranch> civilBranchGroup = getBranches(branches,
                "Civil Engineering");

        List<EngineeringBranch> mechatronicsGroup = getBranches(branches,
                "Mechanical Engineering", "Electrical and Electronics Engineering",
                "Electronics and Communication Engineering", "Computer Science and Engineering");

        // ═══════════════════════════════════════════════════════════════════════════
        // 1. AI / MACHINE LEARNING (Healthcare)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Clinical Decision Support for Early Sepsis Risk Stratification in ICU Telemetry",
                "Machine learning clinical surveillance pipeline that processes continuous multi-parameter physiological streams (heart rate, MAP, SpO2, respiration) and laboratory values to predict septic shock onset 4–6 hours prior to clinical manifestation.",
                getDomain(domains, "AI / Machine Learning", "Artificial Intelligence and Machine Learning"),
                cseBranchGroup,
                getSkills(skills, "Python", "NumPy", "Pandas", "Machine Learning", "Deep Learning", "Statistics", "Scikit-learn"),
                getTechs(technologies, "Python ML", "Scikit-learn"),
                "Sepsis claims over 11 million lives annually worldwide. Mortality increases by 7.6% for every hour antimicrobials are delayed after septic shock onset. Clinical ICU alarms suffer from high false-positive rates (>85%), causing alarm fatigue and delayed intervention.",
                "ICU intensivists, clinical nursing staff, and hospital patient safety officers managing continuous bedside telemetry monitors.",
                "Sliding-window feature extraction on multi-channel vitals, missing-data imputation via forward-fill and spline interpolation, followed by an ensemble gradient boosting classifier and temporal recurrent network with SHAP explainability overlays for bed-side triage.",
                "Area Under ROC Curve (AUC-ROC) >= 0.86; 4-hour lead time prior to clinical sepsis onset; reduction in false alert frequency by >= 35% compared to baseline SIRS criteria.",
                "Reduces hospital length of stay, prevents multi-organ dysfunction syndrome (MODS), and significantly improves clinical intervention timeliness in critical care units.",
                "Combines physiological time-series trend extraction with tree-based SHAP feature attribution directly explaining which physiological delta drove the risk score.",
                "MIMIC-IV clinical database (PhysioNet) and PhysioNet Sepsis Challenge benchmarks containing de-identified hourly vitals, laboratory panels, and demographic data.",
                "FR1: Ingest hourly physiological telemetry feeds. FR2: Handle irregular sampling and missing labs. FR3: Generate 0-100 risk probability every 60 minutes. FR4: Display top contributing features.",
                "NFR1: Inference latency < 250ms per patient window. NFR2: Zero patient data leakage outside memory boundary. NFR3: Fault-tolerant stream processing with audit logging.",
                "Requires MIMIC-IV Data Use Agreement compliance; assumes vital sign sensors have baseline calibration; risk of false negatives during acute physiological transitions.",
                "5-fold cross-validation with group-splitting by patient ID to prevent data leakage; calibration curves (Brier score < 0.12); prospective simulated playback testing.",
                "HIPAA-compliant de-identification; encrypted data stores (AES-256); role-based clinician authorization; failsafe default to standard nursing alerts if model service drops.",
                "Stateless microservice deployment packaged in Docker containers; horizontal scaling supporting 500+ concurrent ICU telemetry beds.",
                "Low-cost software layer utilizing existing hospital telemetry monitors without requiring proprietary bedside hardware.",
                "High clinical translational relevance; deep algorithmic work on imbalanced time-series classification and clinical feature engineering.",
                "ADVANCED", "UG_AND_PG", 2, 4, 16,
                "Understanding of clinical physiology basics, time-series data structures, and ensemble machine learning algorithms."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 2. AI / MACHINE LEARNING (Agriculture)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Multispectral Crop Stress and Nitrogen Deficiency Detection System for Precision Agriculture",
                "Computer vision and deep learning pipeline analyzing low-altitude drone multispectral imagery to isolate early-stage nitrogen deficiency and water stress in staple crop canopies.",
                getDomain(domains, "AI / Machine Learning", "Artificial Intelligence and Machine Learning"),
                cseBranchGroup,
                getSkills(skills, "Python", "NumPy", "Pandas", "Scikit-learn", "Machine Learning", "Deep Learning", "TensorFlow/PyTorch"),
                getTechs(technologies, "Python ML", "TensorFlow/PyTorch"),
                "Excessive or poorly timed nitrogen fertilizer application leads to ground-water nitrate pollution, elevated agricultural expenditure, and greenhouse gas emissions, while under-application causes 15-30% yield penalties. Farmers lack actionable field-level nutrient maps.",
                "Agronomists, farming cooperatives, precision agriculture service providers, and agricultural extension officers.",
                "Orthomosaic tile stitching, radiometric calibration, NDVI/NDRE vegetation index extraction, coupled with a lightweight convolutional segmentation network to output localized nitrogen prescription maps.",
                "Canopy stress classification accuracy >= 91%; localization IoU >= 0.78; variable-rate nitrogen map generated within 90 minutes of flight completion.",
                "Reduces fertilizer overuse by 15-20%, lowers farm operating costs, and mitigates agricultural runoff into regional watersheds.",
                "Dual-branch architecture correlating spectral vegetation indices with spatial texture patterns to distinguish nutrient deficiency from fungal leaf blight.",
                "Open-access drone multispectral datasets (e.g. USDA ARS, PlantVillage, or regional agricultural university field trials) and calibrated orthomosaic imagery.",
                "FR1: Upload RAW multispectral TIFF image sets. FR2: Generate geo-referenced NDVI/NDRE heatmaps. FR3: Classify canopy stress zones. FR4: Export GeoJSON prescription zones.",
                "NFR1: Model footprint < 50MB for potential edge deployment. NFR2: Processing throughput >= 50 hectares/hour. NFR3: Resilient to varied solar illumination angles.",
                "Dependent on cloud cover and flight altitude; requires ground-truth soil or leaf tissue laboratory testing for validation.",
                "Hold-out field validation across three crop growth stages; confusion matrix analysis against destructive SPAD chlorophyll meter measurements.",
                "Protects farm boundary and geolocation privacy; prevents unauthorized land data harvesting.",
                "Batch imagery queueing with scalable background worker pools; exportable to standard farm machinery ISOBUS terminal formats.",
                "Utilizes open-source remote sensing tooling and commodity drone imagery, keeping operational costs low.",
                "Substantial depth in computer vision, radiometric processing, and spatial machine learning.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Familiarity with digital image processing, CNN architectures, and geospatial raster formats."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 3. WEB DEVELOPMENT (Fintech / Banking)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Automated Payment Dispute Resolution and Clearinghouse Settlement Audit Engine",
                "High-throughput enterprise financial transaction audit and dispute reconciliation platform with immutable double-entry ledger bookkeeping and rule-driven chargeback triage.",
                getDomain(domains, "Web Development"),
                cseBranchGroup,
                getSkills(skills, "Java", "Spring Boot", "REST API", "JPA/Hibernate", "SQL/MySQL", "React", "Git", "Maven"),
                getTechs(technologies, "Java Full Stack"),
                "Commercial payment clearinghouses process millions of daily transactions across card networks and UPI. Settlement discrepancies, network drops, and dispute claims require manual case worker investigation taking 7–14 days and costing financial institutions billions in operational overhead.",
                "Payment clearing operations teams, merchant acquirers, dispute resolution analysts, and compliance audit officers.",
                "Event-driven transaction ingest, multi-way reconciliation matching gateway, bank, and merchant settlement files, automated rule triage for chargebacks, and an interactive operational dashboard for dispute tracking.",
                "Reconciles 50,000 transactions/minute with zero double-posting; reduces manual dispute review queue by 60%; 100% audit trail compliance for financial disputes.",
                "Slashes dispute resolution cycle time from 10 days to under 24 hours while preventing fraudulent chargebacks and settlement leakage.",
                "Idempotent multi-phase ledger accounting with deterministic rule engines that differentiate technical dropouts from merchant chargebacks.",
                "Synthetically generated ISO 20022 and card clearing reconciliation files mirroring Visa/Mastercard/UPI settlement batch formats.",
                "FR1: Ingest multi-party settlement batches. FR2: Match records using fuzzy and deterministic joins. FR3: Auto-adjudicate technical reversals. FR4: Provide caseworker review workflow.",
                "NFR1: Strict transactional ACID guarantees. NFR2: API response time < 150ms for dispute filing. NFR3: Complete cryptographic audit trail on all state changes.",
                "Assumes access to standard settlement batch schemas; sensitive to clock synchronization across distributed banking nodes.",
                "Unit testing with JUnit/Mockito; integration test suite validating double-entry invariants; property-based testing for concurrent debit/credit operations.",
                "PCI-DSS aligned data handling; masking of PAN numbers; JWT-based role authorization (analyst, supervisor, auditor); tamper-evident audit logs.",
                "Horizontally scalable Spring Boot microservices backed by connection-pooled MySQL with partitioned transaction tables.",
                "Replaces multi-million dollar commercial settlement engines with a modern open-architecture enterprise stack.",
                "Rigorous software engineering, transaction isolation levels, concurrency control, and distributed systems architecture.",
                "ADVANCED", "UG_AND_PG", 3, 4, 14,
                "Strong foundation in Spring Boot, relational database constraints, ACID transactions, and modern React."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 4. WEB DEVELOPMENT (Supply Chain / Enterprise)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Cold-Chain Pharmaceutical Custody & Real-Time Temperature Excursion Audit System",
                "Full-stack supply chain tracking platform providing end-to-end provenance, temperature compliance verification, and excursion incident workflows for temperature-sensitive biologics.",
                getDomain(domains, "Web Development"),
                cseBranchGroup,
                getSkills(skills, "JavaScript", "React", "Node.js", "Express.js", "MongoDB", "REST API", "Git"),
                getTechs(technologies, "MERN Full Stack"),
                "The WHO estimates up to 50% of vaccines are wasted globally each year largely due to cold chain failures during transit. When temperature breaches occur, consignments are either discarded or administered with compromised efficacy due to lack of verifiable continuous logs.",
                "Pharmaceutical logistics managers, hospital pharmacists, regulatory compliance inspectors, and distribution transport handlers.",
                "Sensor telemetry ingestion service recording temperature/humidity per consignment barcode, rule-based excursion detection, automated quarantine triggers, and a responsive web dashboard for shipment chain-of-custody tracking.",
                "Real-time excursion alert generation within 5 seconds of telemetry receipt; Mean Time to Quarantine reduced from hours to under 2 minutes; complete Good Distribution Practice (GDP) audit reports generated in one click.",
                "Prevents administration of degraded pharmaceuticals, protects public health, and reduces expensive cold-chain insurance write-offs.",
                "Continuous kinetic temperature calculation (Mean Kinetic Temperature - MKT) rather than naive point thresholds, accounting for cumulative biological degradation.",
                "Synthetic and hardware-logged cold chain logger CSV/JSON feeds simulating vaccine transport across inter-modal transit corridors.",
                "FR1: Register pharmaceutical batch barcodes with strict temperature bounds. FR2: Ingest continuous sensor telemetry. FR3: Compute MKT dynamically. FR4: Generate quarantine release workflows.",
                "NFR1: 99.9% telemetry ingestion uptime. NFR2: Sub-second dashboard rendering for active shipment map. NFR3: Immutable log retention for 5 years audit requirements.",
                "Assumes data loggers sync upon gateway connectivity; connectivity dropouts handled with buffered offline batch syncing.",
                "End-to-end integration tests using mock telemetry generators; boundary testing for MKT edge cases; stress testing ingestion endpoints with 1,000 events/sec.",
                "Strict authentication and signature verification on all quarantine override decisions; encrypted data transmission via TLS 1.3.",
                "Node.js cluster workers with optimized document indexing in MongoDB; responsive web interface optimized for warehouse tablets.",
                "Enables regional pharmaceutical distributors to achieve WHO/FDA regulatory compliance without expensive proprietary SaaS subscriptions.",
                "Full-stack software design, time-series data aggregation, domain-driven business logic, and regulatory compliance modeling.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Familiarity with Node.js REST services, React state management, and NoSQL aggregation pipelines."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 5. MOBILE APP DEVELOPMENT (Public Health)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Offline-First Maternal & Child Health Remote Monitoring App for Community Health Workers",
                "Robust cross-platform mobile application engineered for frontline community health workers to record antenatal visits, track child immunizations, and detect high-risk pregnancy indicators in remote rural zones with intermittent cellular connectivity.",
                getDomain(domains, "Mobile App Development", "Mobile Application Development"),
                cseBranchGroup,
                getSkills(skills, "Flutter", "Dart", "Firebase", "REST API", "Android Development"),
                getTechs(technologies, "Flutter", "Android"),
                "In low-resource rural settings, maternal mortality remains disproportionately high due to missed danger signs and incomplete immunization records. Community health workers (ASHAs/ANMs) rely on bulky paper registers that delay timely clinical referrals and cause lost follow-ups.",
                "Community health workers, rural primary health center (PHC) doctors, district health officers, and rural mothers.",
                "Offline-first mobile UI with local encrypted SQLite storage, automated differential conflict-free synchronization when connectivity is restored, automated triage risk scoring (pre-eclampsia, anemia), and SMS referral emergency dispatch.",
                "Zero data loss during multi-week offline usage; conflict resolution accuracy >= 99.8%; identification of high-risk maternal indicators with 100% adherence to WHO triage guidelines.",
                "Empowers frontline health workers to identify obstetrical danger signs early, reducing maternal and infant mortality in underserved geographies.",
                "Deterministic two-way delta sync engine using vector clocks specifically designed for unreliable 2G/EDGE rural cellular networks.",
                "National Health Mission maternal healthcare guidelines, anonymized demographic and immunization schedule datasets.",
                "FR1: Register mother and infant profiles. FR2: Record antenatal vital signs and symptoms offline. FR3: Compute clinical risk category instantly. FR4: Automatic background sync on network restore.",
                "NFR1: Local DB query execution < 50ms. NFR2: Low battery footprint (< 8% per full 8-hour shift). NFR3: High contrast UI accessible in outdoor direct sunlight.",
                "Hardware constraints of low-cost sub-$100 Android smartphones; requires strict validation against duplicate patient registration.",
                "Field simulation testing under intermittent network throttling; automated sync stress tests simulating multi-device record conflicts; usability testing with non-technical users.",
                "Local database encrypted with SQLCipher (AES-256); biometric or PIN app lock; minimal identifiable data transmission.",
                "Built on Flutter for high performance across Android device form factors; backend sync queue capable of handling district-wide batch uploads.",
                "Drastically lowers administrative paperwork and eliminates recurrent printing costs for rural health authorities.",
                "Mobile software engineering, offline-first data synchronization theory, clinical decision support algorithms, and human-computer interaction.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Proficiency in Flutter/Dart, local SQLite storage, mobile state management, and REST APIs."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 6. DATA SCIENCE & ANALYTICS (Municipal Engineering)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Urban Water Distribution Leakage & Non-Revenue Water Detection Engine",
                "Advanced data analytics engine utilizing hydraulic telemetry and acoustic sensor records to identify hidden distribution pipe bursts and anomalous consumption zones across municipal water networks.",
                getDomain(domains, "Data Science & Analytics", "Data Science and Analytics"),
                getBranches(branches, "Computer Science and Engineering", "Information Science and Engineering", "Civil Engineering", "Master of Computer Applications (MCA)"),
                getSkills(skills, "Python", "Pandas", "NumPy", "Statistics", "Machine Learning", "SQL", "Data Visualization"),
                getTechs(technologies, "Python Data Stack", "BI/Analytics"),
                "Municipalities lose 30-45% of treated drinking water as Non-Revenue Water (NRW) through underground pipe leaks and unauthorized draw-offs. Traditional leak detection relies on reactive public complaints after sinkholes develop, causing massive resource waste and road infrastructure damage.",
                "Municipal water supply engineers, urban utility operations managers, and city infrastructure maintenance teams.",
                "Automated Night Flow (MNF) baseline estimation, pressure-flow correlation modeling, spatial anomaly clustering via Isolation Forests, and interactive GIS-linked leak priority triage for repair crews.",
                "Detects pipe bursts >= 15 liters/min within 3 hours; reduces unlocated leakage volume by 25%; pinpoints leak search radius to within 50 meters of actual rupture.",
                "Conserves millions of liters of treated potable water, reduces pumping energy costs, and prevents subterranean soil erosion under municipal roadways.",
                "Combines physics-based hydraulic head loss models with unsupervised machine learning to differentiate normal peak diurnal consumption from subterranean bursts.",
                "Open municipal SCADA pressure and flow datasets (e.g. Battle of the Water Networks benchmark, public utility open telemetry).",
                "FR1: Ingest district metered area (DMA) pressure and flow time-series. FR2: Establish dynamic diurnal consumption baselines. FR3: Flag localized pressure drops. FR4: Rank pipeline segments by leak probability.",
                "NFR1: Anomaly detection pipeline execution < 15 minutes per daily batch. NFR2: Interactive map rendering for 10,000+ pipeline nodes in < 2 seconds.",
                "Assumes baseline sensor calibration; requires reliable GIS pipeline network topology data.",
                "Benchmarked against standard hydraulic simulation benchmarks (EPANET); historical burst case replay verification.",
                "Access control restricting municipal utility infrastructure topology from public disclosure.",
                "Modular Python analytics stack exportable to automated Airflow pipelines and cloud analytics warehouses.",
                "Delivers enterprise-grade leak analytics using open-source tooling, accessible to budget-constrained municipal city councils.",
                "Combines hydraulic engineering principles, statistical process control, and spatial machine learning.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Understanding of statistical time-series analysis, Python pandas/numpy, and GIS spatial data concepts."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 7. CYBER SECURITY (Zero Trust / Enterprise)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Zero-Trust Microsegmentation Policy Validator & Lateral Movement Threat Detector",
                "Security automation platform that simulates adversary lateral movement paths across virtualized cloud networks and audits microsegmentation firewall rules against least-privilege principles.",
                getDomain(domains, "Cyber Security"),
                cseBranchGroup,
                getSkills(skills, "Networking", "Linux", "Cryptography", "Network Security", "Web Security", "Ethical Hacking"),
                getTechs(technologies, "Network Security", "Ethical Hacking"),
                "Traditional perimeter defense security models fail once an attacker compromises an endpoint or credential. Attackers dwell undetected for an average of 200+ days, pivoting laterally across flat subnets to reach crown-jewel databases and domain controllers.",
                "Security Operations Center (SOC) analysts, enterprise network security architects, and compliance officers.",
                "Network flow log parser, directed attack graph constructor utilizing MITRE ATT&CK lateral movement techniques, automated reachability analysis, and policy gap detection recommending exact firewall microsegmentation rules.",
                "Identifies 100% of unintended cross-tier lateral paths; validates 10,000 firewall rules in under 60 seconds; provides remediation syntax for Kubernetes NetworkPolicies and AWS Security Groups.",
                "Prevents enterprise ransomware propagation, protects sensitive customer databases, and achieves compliance with zero-trust mandates.",
                "Graph-theoretic reachability modeling combined with dynamic taint analysis on identity-to-workload authorization tokens.",
                "Public network flow datasets (e.g. CICIDS, PCAP repositories) and synthetic enterprise active directory topologies.",
                "FR1: Ingest NetFlow/VPC flow records. FR2: Construct directed network communication topology. FR3: Simulate lateral pivot trajectories. FR4: Generate least-privilege network policies.",
                "NFR1: Capable of parsing 500,000 flow records in < 30 seconds. NFR2: Zero modification to production network traffic during analysis.",
                "Assumes flow logs reflect representative operational traffic; requires accurate service-to-asset criticality tagging.",
                "Penetration testing validation in an isolated virtualization sandbox; verification against known CVE lateral movement attack vectors.",
                "Secure credential management; role-based dashboard access; audit trail of all policy generation events.",
                "Lightweight Go/Python engine designed for standalone CLI or integrated CI/CD deployment pipeline execution.",
                "Replaces costly enterprise security posture management tools with an auditable open-source framework.",
                "Deep networking fundamentals, graph algorithms, zero-trust architecture, and security exploit mechanics.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Strong grasp of TCP/IP, subnetting, Linux networking, firewall mechanisms, and basic graph algorithms."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 8. CLOUD COMPUTING (Resilience & Disaster Recovery)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Multi-Cloud Disaster Recovery Orchestration Engine with Automated State Reconciliation",
                "Automated disaster recovery engine monitoring primary cloud service availability and executing cross-cloud database replication verification and DNS failover with minimal Recovery Point Objective (RPO).",
                getDomain(domains, "Cloud Computing"),
                cseBranchGroup,
                getSkills(skills, "Linux", "Networking", "AWS", "Docker", "Kubernetes", "Cloud Security"),
                getTechs(technologies, "AWS", "Containers"),
                "Major cloud provider availability zone outages cause extensive downtime for mission-critical digital services. Manual failover processes are error-prone, resulting in data inconsistency, split-brain scenarios, and RTOs exceeding acceptable business thresholds.",
                "Site Reliability Engineers (SREs), DevOps architects, and cloud infrastructure engineers.",
                "Distributed heartbeat monitor with quorum consensus, cross-cloud asynchronous database replication lag tracker, automated DNS failover switcher, and automated state reconciliation runbook execution.",
                "Automated failover execution time (RTO) < 90 seconds; data replication loss window (RPO) < 30 seconds; 0% false failovers during transient network blips.",
                "Guarantees continuous uptime for financial, healthcare, and enterprise web applications during catastrophic cloud outages.",
                "Quorum-based multi-region consensus health checking preventing split-brain DNS cutovers, combined with automated post-failover data consistency verification.",
                "Synthetic application workloads and cloud telemetry metrics gathered from simulated multi-cloud environments.",
                "FR1: Continuous health checks across multi-cloud endpoints. FR2: Monitor database replication offset. FR3: Execute automated DNS switch. FR4: Rollback and reconcile dirty writes upon primary recovery.",
                "NFR1: Orchestrator agent resource footprint < 256MB RAM. NFR2: High fault tolerance with no single point of failure in monitor daemon.",
                "Assumes secondary cloud infrastructure is provisioned or autoscale-ready; dependent on public DNS propagation constraints.",
                "Chaos engineering testing using simulated network partitions; dry-run failover tests against active multi-region testbeds.",
                "Strict IAM least-privilege roles; encrypted state stores; mutual TLS between monitoring heartbeats.",
                "Containerized microservice orchestrator deployable across any compliant Kubernetes cluster.",
                "Prevents vendor lock-in and mitigates multi-million dollar business downtime costs.",
                "Distributed systems design, consensus protocols, cloud networking, and reliability engineering.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Familiarity with Docker, Kubernetes, Linux administration, and cloud networking fundamentals."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 9. IOT (Smart Cities / Environmental Sensing)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Distributed Low-Power LoRaWAN Air Quality Monitoring & Environmental Hazard Early Warning Mesh",
                "Battery-efficient, outdoor environmental sensing network deploying LoRaWAN wireless nodes for particulate matter (PM2.5, PM10) and toxic gas telemetry across urban and industrial zones.",
                getDomain(domains, "IoT"),
                getBranches(branches, "Electronics and Communication Engineering", "Electrical and Electronics Engineering", "Computer Science and Engineering"),
                getSkills(skills, "Sensors", "Arduino", "ESP32", "MQTT", "Embedded C", "IoT Protocols"),
                getTechs(technologies, "ESP32", "Arduino"),
                "Urban pollution hotspots vary drastically block-by-block, yet municipal monitoring stations are sparsely located miles apart. Industrial emissions and sudden hazardous chemical or wildfire smoke surges go unnoticed until regional air quality deteriorates severely.",
                "Municipal environmental officers, industrial park safety managers, and community public health researchers.",
                "Solar-harvesting low-power sensor nodes measuring PM2.5, NO2, CO, and temperature/humidity, transmitting over LoRaWAN sub-gigahertz mesh to a central gateway, with cloud ingestion, spatial interpolation, and automated public alert thresholds.",
                "Node battery autonomy >= 180 days without direct sunlight; wireless packet delivery ratio >= 96% over 3km urban radius; hazard alert broadcast within 15 seconds of threshold breach.",
                "Provides localized high-resolution pollution exposure data, identifying illegal emissions and protecting vulnerable populations during severe smog episodes.",
                "Adaptive duty-cycling algorithm that dynamically increases transmission frequency only when air quality gradient shifts rapidly, preserving battery life during stable conditions.",
                "Calibrated optical dust sensor and electrochemical gas sensor outputs verified against reference meteorological stations.",
                "FR1: Collect calibrated PM2.5/PM10 and gas readings every 5 minutes. FR2: Transmit encrypted LoRaWAN packets. FR3: Aggregate data at gateway and publish via MQTT. FR4: Render real-time spatial heatmaps.",
                "NFR1: Node power consumption < 15uA in deep sleep. NFR2: Weatherproof IP65 enclosure design. NFR3: Ingestion service handles 100+ concurrent sensor streams.",
                "Sensors require periodic drift calibration; physical obstruction can attenuate sub-GHz RF signals in dense urban canyons.",
                "RF range testing in varied urban topographies; side-by-side chamber calibration against laboratory grade reference instruments.",
                "LoRaWAN AES-128 network and application session encryption; secure gateway firmware authentication.",
                "Scalable gateway architecture allowing addition of dozens of nodes without recurring SIM card cellular fees.",
                "Built with low-cost open hardware components, costing less than 5% of conventional commercial monitoring stations.",
                "Combines embedded firmware design, RF wireless propagation, low-power sensor interfacing, and cloud IoT telemetry.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Understanding of microcontrollers, I2C/SPI sensor interfacing, and wireless IoT communication."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 10. EMBEDDED SYSTEMS (Automotive / EV)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "CAN Bus Telematics Gateway for Electric Vehicle Battery Health & Thermal Runaway Alerting",
                "Automated embedded ECU firmware gateway intercepting CAN bus vehicle frames to continuously compute Lithium-Ion cell degradation, state-of-health (SoH), and generate early thermal runaway alerts.",
                getDomain(domains, "Embedded Systems"),
                getBranches(branches, "Electronics and Communication Engineering", "Electrical and Electronics Engineering", "Mechanical Engineering"),
                getSkills(skills, "C", "Embedded C", "Microcontrollers", "Sensors", "UART/SPI/I2C", "RTOS"),
                getTechs(technologies, "Embedded C", "Microcontrollers"),
                "Lithium-ion battery failures in electric two-wheelers and passenger vehicles pose severe fire and explosion risks due to internal cell dendrite shorting or thermal runaway. Drivers receive inadequate warning before catastrophic battery combustion.",
                "EV design engineers, fleet operators, automotive diagnostic technicians, and battery manufacturing quality teams.",
                "Automotive microcontroller (STM32/ARM Cortex-M) running FreeRTOS, interfacing with vehicle CAN 2.0B / CAN-FD network, decoding BMS cell voltages and temperatures, executing equivalent circuit battery state-of-charge/health estimation, and broadcasting priority safety alerts.",
                "Thermal runaway pre-warning issued >= 5 minutes prior to critical temperature runaway; CAN message processing latency < 5ms; zero missed high-priority emergency frames.",
                "Prevents catastrophic EV battery fires, enhances passenger safety, and optimizes vehicle battery pack longevity.",
                "Dual-criteria thermal modeling combining cell surface temperature rate-of-change (dT/dt) with voltage drop anomalies to detect internal micro-shorts before thermal runaway is irreversible.",
                "Real and simulated automotive CAN logs (SAE J1939 / ISO 11898) capturing normal driving, regenerative braking, and thermal abuse states.",
                "FR1: Sniff and decode CAN bus packets at 500kbps. FR2: Extract individual cell voltages and thermistor temps. FR3: FreeRTOS task prioritizing safety alerts. FR4: Output visual/audio driver alarm and cloud telematics.",
                "NFR1: Strict real-time determinism with zero CAN buffer overrun. NFR2: Automotive temperature range tolerance (-40C to +85C). NFR3: Hardware watchdog recovery < 50ms.",
                "Dependent on accurate BMS CAN message dictionary (DBC file); requires isolation from high-voltage traction bus.",
                "Hardware-In-the-Loop (HIL) testing using CAN log playback tools; fault injection testing for sensor disconnects and bus-off recovery.",
                "CAN bus message authentication to prevent spoofing; fail-safe default state for all alarm outputs.",
                "Firmware engineered for standard automotive Tier-1 MCUs; easily ported across vehicle platforms.",
                "Uses cost-effective 32-bit automotive microcontrollers rather than high-end telematics computers.",
                "Deep embedded real-time systems programming, automotive bus protocols, and electro-chemical battery safety modeling.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Strong C programming skills, FreeRTOS experience, and knowledge of automotive communication protocols."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 11. VLSI / DIGITAL DESIGN (Hardware Security)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Hardware-Accelerated Cryptographic Coprocessor for Edge IoT Security on FPGA",
                "Synthesizable RTL implementation in Verilog of an area-optimized AES-256 and SHA-3 cryptographic accelerator with side-channel countermeasures for low-power edge computing.",
                getDomain(domains, "VLSI / Digital Design", "VLSI and Digital Systems"),
                getBranches(branches, "Electronics and Communication Engineering"),
                getSkills(skills, "Digital Electronics", "Boolean Logic", "Verilog", "VHDL", "FPGA", "CMOS", "RTL Design"),
                getTechs(technologies, "Verilog", "FPGA"),
                "Software implementations of symmetric cryptography on resource-constrained microcontrollers consume excessive CPU cycles and battery power, and remain highly susceptible to power analysis side-channel attacks (DPA).",
                "Semiconductor design engineers, secure microcontroller architects, and defense/aerospace hardware security teams.",
                "Pipelined Verilog RTL design of AES-256 cipher engine with round-key generation, SHA-3 sponge function, AXI4-Lite bus interface, and random jitter clock masking for side-channel defense, verified on Xilinx Artix-7 FPGA.",
                "Throughput >= 1.2 Gbps at 100MHz clock; logic footprint <= 3,500 LUTs; 8x energy efficiency gain compared to optimized software crypto on ARM Cortex-M4.",
                "Provides hardware-root-of-trust encryption for connected medical devices, smart meters, and autonomous edge drones.",
                "Resource-sharing architecture reusing substitution-box (S-Box) logic between encryption and decryption paths, achieving minimum silicon area.",
                "NIST FIPS-197 AES test vectors and FIPS-202 SHA-3 known-answer test (KAT) suites.",
                "FR1: Synthesizable Verilog module for AES-256 encryption/decryption. FR2: Synthesizable SHA-3 hashing module. FR3: AXI4-Lite memory-mapped register interface. FR4: Side-channel power masking logic.",
                "NFR1: Fully synchronous design with clean timing closure at 100MHz. NFR2: Zero combinational loops and setup/hold timing violations. NFR3: Verifiable in standard RTL simulation.",
                "FPGA target resource constraints; requires careful clock domain crossing (CDC) verification.",
                "Comprehensive ModelSim / Vivado testbenches verifying 100% of NIST test vectors; static timing analysis (STA) post-implementation.",
                "Protection against timing attacks via constant-time execution; side-channel power obfuscation.",
                "Synthesizable IP core ready for integration into RISC-V SoC microarchitectures.",
                "Drastically lower unit cost in ASIC production compared to dedicated commercial crypto silicon.",
                "RTL architecture design, finite state machines, pipelining, timing analysis, and hardware cryptography.",
                "ADVANCED", "UG_AND_PG", 2, 3, 14,
                "Strong grasp of digital logic design, Verilog HDL, FSMs, and FPGA synthesis workflows."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 12. ROBOTICS & AUTOMATION (Intralogistics)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Autonomous Mobile Inventory Scanning Robot with 2D LiDAR SLAM & Obstacle Avoidance",
                "Differential-drive Autonomous Mobile Robot (AMR) incorporating ROS, 2D LiDAR Simultaneous Localization and Mapping (SLAM), and optical barcode recognition for automated warehouse stock auditing.",
                getDomain(domains, "Robotics & Automation"),
                mechatronicsGroup,
                getSkills(skills, "C/C++", "Sensors", "Motors & Actuators", "Control Systems", "ROS", "Arduino"),
                getTechs(technologies, "Robotics", "ROS"),
                "Manual inventory stock audits in distribution warehouses are labor-intensive, hazardous at high racking levels, and prone to 2-4% human misplacement errors that disrupt order fulfillment.",
                "Warehouse logistics managers, inventory audit supervisors, and automated fulfillment center operators.",
                "Hardware mobile chassis equipped with optical encoders, wheel motors, 2D LiDAR, and camera, running ROS Navigation Stack (Nav2) with Cartographer SLAM, performing autonomous waypoint inspection of warehouse shelf locations.",
                "Localization accuracy within +/- 3 cm; obstacle detection and path replanning within 200ms; barcode inventory scan accuracy >= 99.5%.",
                "Automates repetitive warehouse audits, reduces stock-out discrepancy costs, and improves warehouse worker occupational safety.",
                "Adaptive costmap inflation that dynamically slows vehicle speed and recalculates clearance when human forklifts or workers are detected in aisle paths.",
                "Simulated Gazebo warehouse environments and real-world indoor office/laboratory testing corridors.",
                "FR1: Autonomous indoor SLAM map creation. FR2: Global and local path planning with A* and TEB local planner. FR3: Obstacle avoidance. FR4: Shelf barcode acquisition and logging.",
                "NFR1: Real-time control loop frequency >= 50Hz. NFR2: Minimum continuous runtime >= 90 minutes. NFR3: Emergency stop response < 100ms.",
                "Dependent on lighting conditions for optical barcodes; LiDAR performance degraded in high-glare environments.",
                "Simulation verification in Gazebo followed by physical path tracking repeatability testing; closed-loop PID velocity tuning.",
                "Dual-channel hardware emergency stop (E-Stop) physical switch; software safety boundary limits on motor torque.",
                "ROS2 modular node architecture allowing straightforward addition of RFID or 3D vision sensors.",
                "Built using open robotics software and modular off-the-shelf components at a fraction of commercial AMR cost.",
                "Mobile robot kinematics, sensor fusion, SLAM algorithms, and real-time robotic middleware.",
                "ADVANCED", "UG_AND_PG", 3, 4, 16,
                "Knowledge of C++/Python, ROS/ROS2 concepts, basic kinematics, and microcontroller interfacing."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 13. ELECTRICAL POWER SYSTEMS (Smart Grid)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Substation Microgrid Islanding & Black-Start Coordination System with PMU Telemetry",
                "Smart grid automation model and controller coordinating intentional microgrid islanding, frequency stabilization, and black-start restoration using Phasor Measurement Unit (PMU) telemetry.",
                getDomain(domains, "Electrical Power Systems"),
                eeeBranchGroup,
                getSkills(skills, "Electrical Circuits", "Power Systems", "Transformers", "Generators", "Transmission & Distribution", "Power System Protection", "MATLAB/Simulink"),
                getTechs(technologies, "Power Systems", "MATLAB/Simulink"),
                "Extreme weather and transmission line faults trigger cascading grid collapses. When the main utility grid fails, local microgrids struggle to transition seamlessly to islanded mode without tripping due to severe voltage and frequency transients.",
                "Substation protection engineers, microgrid operators, and municipal electric utility dispatchers.",
                "Simulink/MATLAB dynamic power system model comprising synchronous generation, solar PV inverters, and battery storage, implementing synchrophasor-based islanding detection (ROCOF) and automated load-shedding sequence control.",
                "Islanding detection time < 40ms; frequency deviation maintained within +/- 0.5 Hz during disconnection; black-start restoration sequence completed under 5 minutes.",
                "Prevents prolonged power outages for critical infrastructure (hospitals, water pumping stations) during wider grid blackout events.",
                "Integrated Rate of Change of Frequency (ROCOF) with reactive power excursion triggers to eliminate non-detection zones in islanding recognition.",
                "IEEE 14-bus and IEEE 33-bus standard distribution test feeder models with historical synchrophasor anomaly recordings.",
                "FR1: Continuous monitoring of bus voltage and frequency vectors. FR2: High-speed anti-islanding trip logic. FR3: Priority load-shedding control. FR4: Black-start battery inverter synchronization.",
                "NFR1: Control loop response under 10ms. NFR2: Adherence to IEEE 1547 interconnection standards for distributed energy resources.",
                "Assumes availability of fast-acting energy storage to buffer sudden generation-load mismatches.",
                "Extensive dynamic simulation across diverse fault impedances, motor starting events, and renewable intermittency scenarios in MATLAB/Simulink.",
                "Fail-safe protection relay interlocks preventing out-of-phase reclosure with utility grid.",
                "Controller logic structured for export to real-time digital simulator (RTDS) and commercial PLC platforms.",
                "Maximizes utilization of existing distributed energy assets without requiring massive substation overhauls.",
                "Power system dynamics, transient stability, synchrophasor analytics, and protection relay coordination.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Strong background in AC power systems, three-phase circuits, and MATLAB/Simulink modeling."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 14. RENEWABLE ENERGY (Solar PV / Storage)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Adaptive Solar PV Maximum Power Point Tracking (MPPT) with Battery State-of-Health Optimizer",
                "Advanced power electronic converter controller executing fast-converging Maximum Power Point Tracking under partial shading conditions while coordinating multi-stage battery charging to extend cell cycle life.",
                getDomain(domains, "Renewable Energy"),
                eeeBranchGroup,
                getSkills(skills, "Solar Energy", "Solar PV", "Power Electronics", "Energy Storage", "Battery Systems", "Smart Grid"),
                getTechs(technologies, "Solar", "Smart Grid"),
                "Conventional perturb-and-observe (P&O) MPPT algorithms get trapped in local power maxima under partial cloud shading, losing 20-35% of available solar generation. Furthermore, uncoordinated solar charging accelerates lithium battery degradation through thermal and voltage stress.",
                "Solar microgrid installers, renewable energy project developers, and off-grid facility engineers.",
                "DC-DC synchronous buck-boost converter topology controlled by a hybrid Global MPPT algorithm (Particle Swarm + incremental conductance), coupled with an adaptive CC-CV charging profile tuned to battery internal resistance.",
                "Solar energy capture efficiency >= 98.2% under dynamic partial shading; MPPT convergence time < 250ms; estimated 25% extension in battery pack cycle life.",
                "Increases renewable energy harvest from existing solar installations and reduces high battery replacement expenditures in off-grid solar systems.",
                "Unified control algorithm that shifts MPPT operating point slightly when battery state-of-health diagnostics indicate elevated thermal stress.",
                "Solar irradiance curves from regional solar telemetry databases and manufacturer PV module P-V/I-V characteristic curves.",
                "FR1: Rapid tracking of global maximum power point under partial shading. FR2: DC-DC converter PWM duty cycle adjustment. FR3: Multi-stage battery charging state machine. FR4: Over-voltage and reverse-current protection.",
                "NFR1: Converter electrical efficiency >= 94%. NFR2: Fast transient recovery during abrupt solar irradiance step changes.",
                "Sensitive to MOSFET switching losses and inductor core saturation at high current levels.",
                "Simulink power electronics simulation followed by experimental prototype testing on a 250W test bench with programmable solar simulator.",
                "Hardware over-current protection, thermal cut-off fuses, and galvanic isolation between power and control stages.",
                "Control firmware designed for low-cost digital signal controllers (e.g. TI C2000 or STM32).",
                "High return-on-investment by extracting more kilowatt-hours from identical solar panel hardware.",
                "Power electronics design, digital control theory, solar photovoltaic physics, and electrochemical storage mechanics.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Understanding of DC-DC converters, switching power supplies, and solar panel I-V curves."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 15. ELECTRICAL DRIVES & CONTROL (EV Traction)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Field-Oriented Control (FOC) Permanent Magnet Synchronous Motor Drive for Light Electric Vehicles",
                "High-performance Field-Oriented Control (FOC) inverter drive system for Permanent Magnet Synchronous Motors (PMSM), delivering smooth torque response, wide speed range via flux weakening, and regenerative braking energy recovery.",
                getDomain(domains, "Electrical Drives & Control"),
                getBranches(branches, "Electrical and Electronics Engineering", "Mechanical Engineering"),
                getSkills(skills, "Control Systems", "Power Electronics", "Electric Motors", "Drives", "Industrial Automation", "MATLAB/Simulink"),
                getTechs(technologies, "Electrical Drives", "Industrial Automation"),
                "Light commercial electric three-wheelers and scooters suffer from low powertrain efficiency and jerky torque delivery when using primitive scalar (V/f) or trapezoidal BLDC commutation, reducing range and vehicle driveability.",
                "EV powertrain manufacturers, electric mobility retrofitters, and motor drive R&D teams.",
                "Three-phase voltage source inverter with space vector pulse width modulation (SVPWM), Clarke/Park transformations, dual PI current loops, and high-speed rotor position observer, simulated and tuned for high dynamic response.",
                "Torque ripple reduced to < 4.5%; overall drive efficiency >= 92% across drive cycle; regenerative braking recaptures up to 18% of kinetic deceleration energy.",
                "Extends EV driving range per kilowatt-hour, improves driver ride comfort, and reduces mechanical wear on friction braking pads.",
                "Seamless transition between maximum-torque-per-ampere (MTPA) and flux-weakening operation extending high-speed highway range.",
                "Standard automotive drive cycle profiles (ECE-15, WLTC) and physical PMSM motor parameter datasheets.",
                "FR1: Execute Clarke and Park transformations at 10kHz. FR2: Decoupled direct (d) and quadrature (q) axis current control. FR3: SVPWM duty cycle generation. FR4: Smooth regenerative braking torque blending.",
                "NFR1: Current loop execution time < 50 microseconds. NFR2: Failsafe inverter shutdown within 5 microseconds upon over-current detection.",
                "Requires accurate motor parameter knowledge (stator resistance, d-q inductances) for optimal observer performance.",
                "Simulation validation in MATLAB/Simulink Simscape Electrical followed by DSP code generation and dynamometer bench testing.",
                "Hardware dead-time insertion preventing shoot-through; galvanic optocoupler isolation on gate drive signals.",
                "Firmware architecture compatible with standard 32-bit automotive motor control microcontrollers.",
                "Provides Tier-1 level motor control performance using open control architectures without proprietary supplier licensing fees.",
                "AC motor field theory, digital signal processing, vector control mathematics, and inverter power electronics.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Strong foundation in electric machines, power semiconductor switching, and control system mathematics."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 16. CAD / PRODUCT DESIGN (Ergonomics / Biomechanics)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Modular Industrial Upper-Limb Exoskeleton for Assembly Line Musculoskeletal Strain Reduction",
                "Parametric CAD design, biomechanical stress analysis, and structural topology optimization of a passive upper-limb wearable exoskeleton mechanism engineered to alleviate shoulder and lumbar fatigue in overhead assembly workers.",
                getDomain(domains, "CAD / Product Design"),
                mechBranchGroup,
                getSkills(skills, "Engineering Drawing", "AutoCAD", "SolidWorks", "CATIA", "3D Modeling", "GD&T", "Product Design"),
                getTechs(technologies, "SolidWorks", "CATIA"),
                "Work-related musculoskeletal disorders (WMSDs) from prolonged overhead manual tool handling are the leading cause of chronic disability and lost workdays among automotive and aerospace manufacturing assembly technicians.",
                "Manufacturing plant ergonomics officers, occupational health safety inspectors, and automotive assembly plant operators.",
                "Kinematic multi-link shoulder joint mechanism with cam-spring counterbalance torque curve, designed in SolidWorks/CATIA with GD&T drawings, validated via Finite Element Analysis (FEA) for fatigue life, and 3D printed for physical fit testing.",
                "40% reduction in peak deltoid muscle strain during overhead tool holding; total exoskeleton weight <= 2.8 kg; fatigue life >= 500,000 motion cycles without structural failure.",
                "Dramatically reduces worker shoulder injuries, cuts industrial workers' compensation claims, and increases end-of-shift assembly throughput.",
                "Non-linear progressive torque cam geometry that provides maximum gravitational compensation only between 90 and 150 degrees of arm elevation without resisting downward arm motion.",
                "Biomechanical human anthropometric databases (NASA-STD-3000, ISO 7250) and material yield/fatigue curves for carbon fiber and aerospace aluminum 7075-T6.",
                "FR1: Full parametric 3D CAD assembly with human kinematic range of motion (ROM). FR2: FEA static and fatigue stress simulation. FR3: Adjustability for 5th to 95th percentile worker body sizes. FR4: Manufacturing drawings with GD&T.",
                "NFR1: Factor of safety >= 2.0 under maximum dynamic tool load. NFR2: Donning/doffing time < 45 seconds. NFR3: Breathable ergonomic skin contact interface.",
                "Passive mechanical system (no external battery or motors); restricted to shoulder-elevation assistance tasks.",
                "ANSYS/SolidWorks Simulation FEA stress analysis under cyclic load; physical prototype range-of-motion verification and wearable comfort evaluation.",
                "Zero pinch points along human joint kinematic trajectories; quick-release emergency stop buckle system and mechanical travel limits with ergonomic padded interface.",
                "Modular sub-assemblies enabling rapid component replacement in factory maintenance cribs.",
                "Avoids expensive powered robotics, utilizing low-cost springs and optimized lightweight linkages for affordable deployment.",
                "Biomechanical engineering, mechanism synthesis, non-linear cam design, finite element analysis, and manufacturing drafting.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Proficiency in SolidWorks/CATIA, engineering mechanics, strength of materials, and GD&T drafting."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 17. MANUFACTURING (Condition Monitoring)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Acoustic Emission & Vibration Sensor Array for CNC Milling Tool Wear Prognostics",
                "In-process cyber-physical condition monitoring system capturing high-frequency acoustic emissions and spindle vibration to predict cutting tool flank wear and prevent catastrophic workpiece scrap in CNC milling.",
                getDomain(domains, "Manufacturing"),
                mechBranchGroup,
                getSkills(skills, "Manufacturing Processes", "CNC", "CAM", "3D Printing", "Production Planning", "Quality Control", "Industrial Automation"),
                getTechs(technologies, "CNC", "CAM"),
                "Undetected cutting tool wear in precision CNC machining causes dimensional inaccuracy, degraded surface finish (Ra), and catastrophic tool breakage that destroys expensive aerospace/automotive workpieces and damages machine spindles.",
                "Machining shop floor managers, CNC process engineers, and manufacturing quality assurance supervisors.",
                "Piezoelectric accelerometer and acoustic emission sensor mounted on CNC machine spindle, coupled with digital signal feature extraction (RMS, kurtosis, wavelet packet decomposition) to classify tool wear state in real time.",
                "Tool wear state classification accuracy >= 93%; premature tool replacement reduced by 20%; zero catastrophic unpredicted tool breakages during machining trials.",
                "Prevents expensive workpiece scrapping, extends cutting insert utilization to safe physical limits, and minimizes unpredicted spindle downtime.",
                "Multi-modal fusion of acoustic emission burst energy with mechanical vibration spectra to distinguish intermittent chip breakage from continuous flank wear.",
                "Experimental machining sensor datasets (NASA Milling Data, University manufacturing laboratory cutting trials) covering varied feed rates and depths of cut.",
                "FR1: Ingest continuous vibration and acoustic signals during cutting passes. FR2: Segment idle rapids from cutting engagement. FR3: Compute statistical time-frequency wear features. FR4: Trigger tool change alarm before critical wear limit.",
                "NFR1: Sampling frequency >= 100 kHz for acoustic emission channel. NFR2: Real-time decision latency < 100ms per cutting pass.",
                "Sensor placement must not obstruct tool changers or coolant spray; requires baseline training for each specific workpiece material.",
                "Validation across diverse machining parameters (carbon steel, aluminum 6061) using optical microscope flank wear measurements (VB) as ground truth.",
                "Fail-safe automatic feed-hold signal interfaced with CNC controller emergency input.",
                "Modular sensor pod capable of retrofitting existing industrial CNC vertical machining centers.",
                "Saves thousands of dollars annually per CNC machine in avoided tooling waste and reduced scrap parts.",
                "Machining tribology, metal cutting dynamics, sensor signal processing, and statistical quality engineering.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Knowledge of CNC machining operations, cutting tool geometry, and sensor signal acquisition."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 18. AUTOMOTIVE ENGINEERING (Brake-by-Wire)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Electro-Hydraulic Brake-by-Wire Control Unit with Anti-Lock & Regenerative Blending",
                "Model-based design and embedded control architecture for an electro-hydraulic Brake-by-Wire (BBW) system that seamlessly blends electric motor regenerative braking with hydraulic caliper pressure for electric vehicles.",
                getDomain(domains, "Automotive Engineering"),
                getBranches(branches, "Mechanical Engineering", "Electrical and Electronics Engineering"),
                getSkills(skills, "Automotive Systems", "IC Engines", "Vehicle Dynamics", "Automobile Electronics", "AutoCAD", "EV Technology"),
                getTechs(technologies, "Automotive Systems", "EV"),
                "Conventional hydraulic brakes in EVs fail to maximize kinetic energy recovery because the mechanical pedal is rigidly linked to friction calipers, causing harsh transition shudder and energy loss during sudden driver deceleration.",
                "EV chassis engineering teams, automotive active safety system developers, and vehicle dynamics engineers.",
                "Decoupled electronic brake pedal simulator with stroke/force sensing, high-speed linear solenoid valve pressure controller, and regenerative blending controller tuned to keep vehicle slip ratio within the stable tire grip zone.",
                "Stopping distance reduced by 6% on split-friction (mu-split) surfaces; regenerative energy capture increased by up to 22% over fixed-split braking; pedal feel hysteresis within 5% of conventional hydraulic systems.",
                "Significantly improves EV safety during emergency braking while maximizing battery recharging from regenerative deceleration.",
                "Nonlinear model predictive control dynamically distributing brake force between front/rear axles and electric motor according to instantaneous tire normal loads.",
                "Standard vehicle dynamics benchmark parameters (quarter-car and 7-DOF vehicle models) verified against Pacejka Magic Formula tire models.",
                "FR1: Read dual-redundant brake pedal stroke and pressure sensors. FR2: Compute target deceleration. FR3: Command electric powertrain regen torque up to limit. FR4: Modulate electro-hydraulic caliper pressure dynamically.",
                "NFR1: Hydraulic pressure rise time < 120ms to 100 bar. NFR2: Compliance with ISO 26262 ASIL-D fault mitigation requirements.",
                "Requires high-pressure hydraulic accumulator and secondary mechanical failsafe fallback circuit.",
                "Simulink/CarSim co-simulation across dry asphalt, wet road, and ice mu-split braking conditions; Hardware-In-the-Loop test bench validation.",
                "Dual-channel sensor redundancy; default hydraulic pass-through mechanical fail-safe valve in case of electrical power loss.",
                "Packaged into a compact integrated power brake unit suitable for light commercial and passenger EVs.",
                "Consolidates master cylinder, vacuum booster, and ABS/ESC hydraulic modulator into one compact electro-hydraulic unit.",
                "Vehicle longitudinal dynamics, non-linear hydraulic control, regenerative powertrain mechanics, and automotive functional safety.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Strong foundation in vehicle dynamics, hydraulic systems, MATLAB/Simulink, and control engineering."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 19. THERMAL ENGINEERING (Battery Thermal Management)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Phase Change Material (PCM) Thermal Management Enclosure for EV Battery Packs in Tropical Climates",
                "Computational fluid dynamics (CFD) simulation and experimental design of a composite Phase Change Material (PCM) thermal jacket for cylindrical Li-ion cells, preventing thermal runaway and minimizing cell-to-cell temperature disparity during fast-charging in 45°C ambient conditions.",
                getDomain(domains, "Thermal Engineering"),
                mechBranchGroup,
                getSkills(skills, "Thermodynamics", "Heat Transfer", "Fluid Mechanics", "Mechanical Systems", "MATLAB/Simulink"),
                getTechs(technologies, "Thermal Systems", "HVAC"),
                "In tropical climates where summer ambient temperatures reach 40-48°C, fast-charging EV batteries causes rapid internal heat generation. Exceeding 55°C accelerates irreversible battery capacity fading and triggers dangerous cell thermal runaway.",
                "EV battery pack thermal engineers, electric fleet operators in tropical zones, and battery pack design consultants.",
                "Parametric CFD thermal simulation in ANSYS Fluent / OpenFOAM analyzing paraffin wax / expanded graphite composite PCM surrounding 21700 lithium cells, evaluating latent heat absorption, transient temperature profiles, and passive heat dissipation fins.",
                "Maintains maximum cell operating temperature <= 48°C during 2C fast-charging in 45°C ambient; keeps inter-cell temperature variation <= 3.5°C across the entire pack; zero parasitic electric power consumption.",
                "Prevents catastrophic battery pack fires in high-temperature climates while prolonging EV battery life without draining battery power on heavy active liquid pumps.",
                "Expanded graphite composite structure that overcomes the inherently poor thermal conductivity of pure paraffin wax, increasing heat transfer rate by 14x.",
                "Empirical thermal properties of commercial organic PCMs and battery heat generation rates (Joule heating + entropic reversible heat) from published battery calorimetry literature.",
                "FR1: Formulate transient 3D heat generation model for battery pack. FR2: Simulate phase change melting front and latent heat absorption. FR3: Optimize composite graphite mass fraction. FR4: Design enclosure structural heat sink.",
                "NFR1: PCM jacket must fit within 15% volume allowance of standard battery pack. NFR2: No liquid leakage during phase change transition (solid-liquid).",
                "Passive PCM has finite heat absorption capacity; requires ambient cool-down periods between consecutive fast-charge cycles.",
                "Mesh convergence study in ANSYS Fluent; experimental validation on a scaled 6-cell battery module with thermocouple data logging.",
                "Selection of flame-retardant PCM composites (UL94 V-0 compliance); containment sealing preventing chemical outgassing.",
                "Directly applicable to modular battery packs for electric two-wheelers, three-wheelers, and commercial delivery vans.",
                "Eliminates heavy liquid cooling radiators, pumps, and hoses, lowering pack manufacturing complexity and maintenance costs.",
                "Transient convective-conductive heat transfer, phase change thermodynamics, CFD modeling, and electrochemical thermal kinetics.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Strong understanding of conduction/convection heat transfer, thermodynamics, and basic CFD simulation software."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 20. STRUCTURAL ENGINEERING (Seismic Analysis)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Seismic Fragility Assessment & Tuned Liquid Column Damper Retrofit for Multi-Storey RCC Structures",
                "Non-linear dynamic time-history structural analysis in ETABS/STAAD.Pro evaluating earthquake vulnerability of mid-rise reinforced concrete structures, paired with optimized roof-mounted Tuned Liquid Column Dampers (TLCD).",
                getDomain(domains, "Structural Engineering"),
                civilBranchGroup,
                getSkills(skills, "Structural Analysis", "Strength of Materials", "RCC Design", "Steel Design", "STAAD.Pro", "ETABS", "AutoCAD"),
                getTechs(technologies, "STAAD.Pro", "ETABS"),
                "Thousands of older multi-storey reinforced concrete buildings in seismically active zones lack modern ductile detailing. Traditional structural retrofitting (steel bracing, concrete jacketing) is disruptive, expensive, and requires building evacuation.",
                "Structural design consultants, municipal disaster management authorities, and civil infrastructure retrofitting engineers.",
                "3D finite element structural model of a G+10 storey RCC framed building in ETABS, non-linear dynamic time-history analysis using historical earthquake ground motions (El Centro, Bhuj), calculation of structural fragility curves, and optimization of roof-mounted U-tube liquid damper dimensions.",
                "Reduces peak lateral roof displacement by 32% during seismic excitation; decreases inter-storey drift ratio to within IS 1893 / ASCE 7 safety limits; TLCD mass tuned to <= 2.5% of total building seismic weight.",
                "Mitigates collapse risk during major seismic events, preserves human life, and allows non-invasive retrofitting without displacing building occupants.",
                "Utilizes standard rooftop water storage tanks configured as Tuned Liquid Column Dampers, simultaneously serving potable water needs and seismic damping.",
                "Standard accelerogram records from PEER Ground Motion Database and codal design spectra from IS 1893:2016 / ASCE 7-16.",
                "FR1: Construct 3D space frame RCC structural model with accurate dead/live loads. FR2: Perform response spectrum and non-linear time history analysis. FR3: Compute inter-storey drift and base shear. FR4: Dimension TLCD orifice and liquid column parameters.",
                "NFR1: Convergence tolerance in non-linear solver < 0.001. NFR2: Adherence to concrete building design codes (IS 456, IS 13920, ACI 318).",
                "TLCD performance is frequency-dependent and requires precise tuning to the building's fundamental natural frequency.",
                "Modal analysis frequency verification; comparison of damped versus undamped response envelopes across multiple ground motion records.",
                "Sloshing liquid wave barrier design to prevent accidental roof overflow; structural integrity verification of roof slab under concentrated damper load.",
                "Adaptable to residential, commercial, and hospital buildings situated in seismic zones IV and V.",
                "Costs less than 10% of traditional external structural steel bracing or foundation base-isolation retrofits.",
                "Structural dynamics, non-linear finite element modeling, earthquake engineering principles, and passive structural vibration control.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Solid grasp of structural mechanics, reinforced concrete design, ETABS/STAAD modeling, and structural dynamics."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 21. CONSTRUCTION TECHNOLOGY (BIM / Carbon Tracking)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "4D BIM-Integrated Construction Progress Verification & Embodied Carbon Tracking System",
                "Digital construction management framework linking 3D Building Information Models (BIM) with project scheduling and automated material takeoff calculations to audit construction milestone progress and embodied carbon emissions.",
                getDomain(domains, "Construction Technology"),
                civilBranchGroup,
                getSkills(skills, "Construction Methods", "Building Materials", "Estimation & Costing", "Project Planning", "Construction Management", "Quantity Surveying", "AutoCAD"),
                getTechs(technologies, "Construction Management"),
                "Construction mega-projects suffer an average of 20% schedule overruns and significant budget escalation due to disconnected planning between 2D drawings, bar charts, and site progress. Furthermore, projects lack transparent tools to measure greenhouse gas emissions from cement and steel usage.",
                "Project management consultants, general contractors, quantity surveyors, and sustainable construction auditors.",
                "Development of a detailed 4D BIM model linking Revit/AutoCAD geometries with Primavera/MS Project schedules, automated calculation of Earned Value Management (EVM) metrics, and real-time embodied carbon estimation using ICE material emission factors.",
                "Schedule variance (SV) and cost variance (CV) tracked with daily granularity; automated Bill of Quantities (BOQ) calculation within 1% error; embodied carbon reduction targets of 15% identified through low-carbon concrete mix alternatives.",
                "Accelerates project delivery, eliminates contractor-client billing disputes, and provides actionable data to achieve green building certifications (LEED/GRIHA).",
                "Integrates live scheduling timelines directly with dynamic embodied carbon accounting across structural milestones.",
                "Architectural and structural building drawing sets, standard schedule work breakdown structures (WBS), and Inventory of Carbon and Energy (ICE) emission databases.",
                "FR1: Generate parametric 3D structural model with material metadata. FR2: Link schedule activities to model elements (4D simulation). FR3: Calculate automated quantity takeoffs. FR4: Generate milestone-by-milestone carbon expenditure curves.",
                "NFR1: 4D model rendering smoothness >= 30fps. NFR2: Adherence to Level of Development (LOD) 350 standards.",
                "Requires consistent naming conventions between BIM object parameters and contractor schedule WBS IDs.",
                "Quantity validation against manual quantity surveying estimates; schedule logic validation preventing circular dependencies.",
                "Role-based access control protecting confidential contractor bid unit rates and commercial contract values.",
                "Open IFC-compliant data schema allowing export to diverse construction ERP systems.",
                "Prevents millions of dollars in liquidated damages from schedule delays and enables cost-effective low-carbon material procurement.",
                "Construction engineering, Building Information Modeling, project management scheduling algorithms, and life-cycle carbon assessment.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Familiarity with construction planning methods, basic BIM concepts, project estimation, and building materials."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 22. GEOTECHNICAL ENGINEERING (Slope Stability)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Rainfall-Triggered Hill Slope Stability Monitoring & Early Warning Landslide Array",
                "Geotechnical numerical modeling and sensor telemetry framework assessing pore water pressure fluctuations and shear strength reduction in hill slopes to predict rainfall-induced slope failure along highway corridors.",
                getDomain(domains, "Geotechnical Engineering"),
                civilBranchGroup,
                getSkills(skills, "Soil Mechanics", "Foundation Engineering", "Soil Testing", "Geotechnical Investigation", "Bearing Capacity", "Slope Stability", "PLAXIS"),
                getTechs(technologies, "Geotechnical Analysis", "PLAXIS"),
                "Rainfall-triggered landslides cause catastrophic loss of life and disrupt mountain highway corridors every monsoon. Conventional geotechnical slope evaluations rely on static factor of safety calculations that fail to account for transient pore water pressure spikes during cloudburst events.",
                "Highway engineering authorities, national disaster response forces, and geotechnical consulting firms.",
                "Limit equilibrium and finite element seepage modeling (PLAXIS/GeoStudio) calibrated against continuous soil moisture and pore water pressure piezometer telemetry, outputting dynamic Factor of Safety (FoS) degradation curves and tiered traffic hazard alerts.",
                "Dynamic Factor of Safety calculated with sub-hourly updates; slope collapse pre-warning issued 2–6 hours before shear surface mobilization; zero failure alarms missed during heavy rainfall events.",
                "Saves lives along mountain transit corridors, enables targeted preventive slope drainage interventions, and guides preemptive highway closures.",
                "Coupled transient unsaturated seepage and slope stability analysis linking antecedent rainfall index directly to real-time geotechnical shear strength degradation.",
                "Geotechnical soil borehole logs, triaxial shear test parameters, soil-water characteristic curves (SWCC), and regional rain gauge datasets.",
                "FR1: Ingest rainfall intensity and pore-water pressure telemetry. FR2: Compute transient unsaturated seepage front. FR3: Calculate instantaneous slope Factor of Safety (Morgenstern-Price / Bishop method). FR4: Broadcast Red/Amber/Green hazard status.",
                "NFR1: Numerical slope stability calculation time < 60 seconds per update cycle. NFR2: Sensor telemetry nodes resistant to lightning and rockfall impacts.",
                "Requires accurate stratigraphy mapping and laboratory soil shear strength parameters from field core drilling.",
                "Model validation against documented historical slope failure cases along mountain highway corridors; mesh refinement testing.",
                "High reliability fail-safe communication alerting highway police when sensor telemetry is lost.",
                "Modular computational engine deployable across multiple vulnerable slope locations in mountainous regions.",
                "Targeted sub-surface horizontal drains recommended by the model cost significantly less than building massive concrete retaining walls along entire road lengths.",
                "Unsaturated soil mechanics, limit equilibrium slope stability theory, finite element seepage analysis, and geotechnical risk management.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Strong foundation in soil mechanics, shear strength theory, geotechnical testing, and numerical modeling."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 23. TRANSPORTATION ENGINEERING (Traffic Control)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Computer Vision-Assisted Adaptive Traffic Signal Controller with Emergency Vehicle Preemption",
                "Intelligent transportation engineering system utilizing real-time traffic camera video feeds to dynamically calculate queue lengths, optimize signal green time splits, and grant immediate green preemption to approaching emergency ambulances.",
                getDomain(domains, "Transportation Engineering"),
                getBranches(branches, "Civil Engineering", "Computer Science and Engineering", "Information Science and Engineering"),
                getSkills(skills, "Highway Engineering", "Traffic Engineering", "Pavement Design", "Transportation Planning", "Road Safety", "GIS"),
                getTechs(technologies, "Transportation Planning", "GIS"),
                "Fixed-time traffic signals cause severe intersection gridlock, wasting fuel in idling vehicles and causing emergency ambulances to be trapped in traffic jams where minutes dictate patient survival.",
                "City traffic police departments, municipal transport planners, and emergency medical ambulance dispatchers.",
                "Edge camera object detection estimating vehicular passenger car units (PCU) per approach leg, Webster's cycle time optimization algorithm dynamically allocating green phases, and RFID/GPS emergency vehicle detection triggering immediate signal preemption.",
                "Reduces average intersection vehicle delay by 28%; cuts ambulance transit time through corridor by up to 45%; eliminates red-light dilemma zone accidents via adaptive yellow extension.",
                "Saves lives during critical emergency medical transfers, reduces urban greenhouse gas emissions from vehicle idling, and optimizes intersection capacity.",
                "Automated emergency preemption recovery algorithm that clears downstream traffic queues before the emergency vehicle arrives, then smoothly restores normal coordination without causing secondary gridlock.",
                "Traffic intersection video camera footage and standard traffic volume count survey benchmark data.",
                "FR1: Detect vehicles and classify into PCU categories (two-wheelers, cars, buses, heavy vehicles). FR2: Calculate instantaneous lane queue lengths. FR3: Adjust green phase splits dynamically. FR4: Preempt phase upon authenticated emergency beacon detection.",
                "NFR1: Video processing frame rate >= 15 fps per intersection leg. NFR2: Fail-safe fallback to standard fixed-time signal plan if camera feed disconnects.",
                "Camera visibility can be degraded in extreme fog or heavy downpours; requires secure vehicle beacon authentication.",
                "Simulation verification in VISSIM / SUMO traffic simulation software across various traffic congestion levels; field trial at a signalized intersection.",
                "Cryptographic signature verification on emergency vehicle beacon signals to prevent unauthorized civilian green signal hacks.",
                "Software controller compatible with existing NEMA TS2 / 2070 traffic signal controller cabinets.",
                "Avoids expensive underground inductive loop detector cutting and cabling, utilizing existing city surveillance cameras.",
                "Traffic flow theory, queueing models, signal timing optimization, computer vision vehicle tracking, and intelligent transportation systems.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Understanding of traffic engineering principles, intersection signal timing, and basic computer vision or Python programming."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 24. ENVIRONMENTAL ENGINEERING (Water Treatment)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Decentralized Greywater Membrane Bioreactor with Solar UV Disinfection for Campus Re-Use",
                "Process design, hydraulic sizing, and automated effluent quality monitoring for a decentralized greywater recycling system utilizing submerged hollow-fiber membrane bioreactors (MBR) and solar UV disinfection to treat hostel and campus wastewater for toilet flushing and landscape irrigation.",
                getDomain(domains, "Environmental Engineering"),
                civilBranchGroup,
                getSkills(skills, "Water Treatment", "Wastewater Treatment", "Solid Waste Management", "Environmental Impact Assessment", "Water Quality", "Environmental Monitoring"),
                getTechs(technologies, "Water Treatment", "Environmental Monitoring"),
                "Educational campuses and residential complexes consume vast amounts of expensive municipal freshwater while discharging hundreds of thousands of liters of lightly contaminated greywater into overburdened city sewers every day, exacerbating urban water scarcity.",
                "Campus estate managers, municipal water reuse authorities, and environmental sustainability officers.",
                "Complete process engineering design: pre-filtration equalization tank, aerobic submerged hollow-fiber MBR tank, automated periodic backwash air scouring, solar-powered multi-lamp UV disinfection chamber, and real-time turbidity/pH telemetry.",
                "Achieves BOD < 5 mg/L, TSS < 2 mg/L, and zero detectable fecal coliforms; treats 25,000 liters/day of greywater; reduces campus fresh municipal water purchase by 35%.",
                "Conserves precious municipal drinking water, reduces campus utility water bills, and prevents nutrient pollution in local water bodies.",
                "Integrated automated transmembrane pressure (TMP) sensor-controlled flux relaxation cycle that triples membrane lifespan and prevents irreversible bio-fouling.",
                "Raw greywater quality parameters (BOD, COD, turbidity, surfactants) sampled from university hostel showers and laundry facilities.",
                "FR1: Hydraulic sizing of equalization, aeration, and permeate storage tanks. FR2: Selection and flux calculation of membrane surface area. FR3: Solar UV dose calculation (minimum 40 mJ/cm2). FR4: Real-time turbidity and dissolved oxygen monitoring.",
                "NFR1: Treated effluent consistently complies with non-potable urban water reuse standards (USEPA / Central Pollution Control Board). NFR2: Low acoustic footprint (< 50 dB) for residential campus surroundings.",
                "Requires periodic chemical cleaning (Clean-In-Place) of membranes; kitchen blackwater containing heavy grease must be excluded.",
                "Laboratory bench-scale pilot MBR testing; standard analytical laboratory water testing (Standard Methods for the Examination of Water and Wastewater).",
                "UV lamp enclosure safety interlock with electrical isolation and over-current protection; automatic shut-off valve diverting permeate to sewer if UV lamp intensity or turbidity sensor detects breach of disinfection standards.",
                "Modular containerized design that can be scaled up or replicated across residential townships and educational institutions.",
                "Estimated financial payback period of 3.2 years based on avoided municipal water purchase tariffs.",
                "Biochemical wastewater treatment kinetics, membrane filtration theory, disinfection kinetics, and environmental impact assessment.",
                "INTERMEDIATE", "UG_AND_PG", 2, 4, 12,
                "Knowledge of environmental engineering, wastewater treatment processes, fluid flow, and chemical water quality parameters."
        );

        // ═══════════════════════════════════════════════════════════════════════════
        // 25. WATER RESOURCES ENGINEERING (Hydrology / Flood Modeling)
        // ═══════════════════════════════════════════════════════════════════════════
        createProject(
                "Catchment-Scale Flash Flood Inundation Modeling & Distributed Retention Basin Sizing",
                "Hydrologic rainfall-runoff simulation and 2D hydrodynamic flood inundation model in HEC-HMS and HEC-RAS to evaluate urban flash flood vulnerabilities and optimize the sizing of distributed stormwater retention basins.",
                getDomain(domains, "Water Resources Engineering"),
                civilBranchGroup,
                getSkills(skills, "Hydrology", "Fluid Mechanics", "Irrigation Engineering", "Hydraulic Structures", "Groundwater", "Flood Management", "Water Resource Planning"),
                getTechs(technologies, "Hydrology", "Hydraulics"),
                "Rapid urban development replaces natural permeable vegetation with impermeable concrete, drastically reducing rainwater infiltration and generating massive peak runoff volumes that overwhelm stormwater drainage networks, flooding urban residential areas.",
                "Municipal storm drainage authorities, urban flood disaster management teams, and civil infrastructure planning bodies.",
                "Catchment delineation using Digital Elevation Models (DEM), rainfall-runoff modeling with SCS-CN method in HEC-HMS, 2D hydraulic overland flood wave propagation in HEC-RAS, and optimization of retention basin storage capacities to attenuate peak outflow below channel capacity.",
                "Reduces peak downstream flood discharge by 38% for a 50-year return period storm event; flood inundation depth maps generated with 5-meter spatial resolution; eliminates waterlogging at critical road underpasses.",
                "Prevents urban inundation damage to homes, businesses, and transport corridors, and facilitates groundwater recharge in water-stressed urban areas.",
                "Coupled 1D sewer pipe network and 2D surface overland flow modeling that simulates urban dual-drainage interaction during manhole overflow events.",
                "High-resolution LiDAR / SRTM Digital Elevation Models, historical daily and hourly rainfall records from meteorological stations, and municipal stormwater drainage GIS shapefiles.",
                "FR1: Extract sub-catchment topographic and land-use parameters. FR2: Generate synthetic design storm hyetographs for 10, 25, and 50-year return periods. FR3: Run 2D unsteady flow hydraulic simulation. FR4: Size distributed retention basins.",
                "NFR1: Mass conservation error in 2D hydraulic simulation < 1.0%. NFR2: Inundation map exportable to GeoTIFF and web GIS viewers.",
                "Dependent on the quality of topographical DEM data; fine micro-features (curbs, road crowns) can alter localized flow paths.",
                "Calibration of Manning's roughness coefficients against recorded historical high-water marks from past flood events.",
                "Emergency overflow spillway sizing to prevent retention basin embankment overtopping and catastrophic breach.",
                "Model structure ready for integration into municipal early warning systems and master drainage plan revisions.",
                "Cost-effective green infrastructure planning avoiding billions in post-flood commercial reconstruction expenses.",
                "Surface hydrology, open channel hydraulics, 2D hydrodynamic numerical modeling, and GIS spatial terrain analysis.",
                "ADVANCED", "UG_AND_PG", 2, 4, 14,
                "Strong foundation in engineering hydrology, open channel flow, fluid mechanics, and HEC-RAS or GIS tools."
        );
    }

    // ─── Helper creation method ──────────────────────────────────────────────

    private void createProject(String title,
                               String description,
                               ProjectDomain domain,
                               List<EngineeringBranch> branches,
                               List<Skill> skills,
                               List<Technology> techs,
                               String problemStatement,
                               String targetStakeholders,
                               String proposedSolution,
                               String expectedOutcomeKpis,
                               String industryValue,
                               String innovationDifferentiator,
                               String dataRequirements,
                               String functionalRequirements,
                               String nonFunctionalRequirements,
                               String constraintsDependencies,
                               String testingValidation,
                               String securityPrivacySafety,
                               String scalabilityIntegration,
                               String costSustainability,
                               String deploymentAcademicDepth,
                               String complexityLevel,
                               String academicLevelSuitability,
                               int minTeamSize,
                               int maxTeamSize,
                               int estimatedWeeks,
                               String prerequisites) {

        // Idempotency check: don't create duplicate projects
        Optional<Project> existing = projectRepository.findByTitleAndIsCatalogTrue(title);
        Project project = existing.orElseGet(Project::new);

        project.setTitle(title);
        project.setDescription(description);
        project.setIsCatalog(true);
        project.setDomain(domain);
        project.setTargetBranches(branches);
        project.setRequiredSkills(skills);

        project.setProblemStatement(problemStatement);
        project.setTargetStakeholders(targetStakeholders);
        project.setProposedSolution(proposedSolution);
        project.setExpectedOutcomeKpis(expectedOutcomeKpis);
        project.setIndustryValue(industryValue);
        project.setInnovationDifferentiator(innovationDifferentiator);
        project.setDataRequirements(dataRequirements);
        project.setFunctionalRequirements(functionalRequirements);
        project.setNonFunctionalRequirements(nonFunctionalRequirements);
        project.setConstraintsDependencies(constraintsDependencies);
        project.setTestingValidation(testingValidation);
        project.setSecurityPrivacySafety(securityPrivacySafety);
        project.setScalabilityIntegration(scalabilityIntegration);
        project.setCostSustainability(costSustainability);
        project.setDeploymentAcademicDepth(deploymentAcademicDepth);
        project.setComplexityLevel(complexityLevel);
        project.setAcademicLevelSuitability(academicLevelSuitability);
        project.setMinTeamSize(minTeamSize);
        project.setMaxTeamSize(maxTeamSize);
        project.setEstimatedWeeks(estimatedWeeks);
        project.setPrerequisites(prerequisites);

        // Evaluate against Quality Gate
        ProjectQualityGateService.QualityGateResult qg = qualityGateService.evaluate(project);
        project.setQualityScore(qg.getQualityScore());
        project.setQualityApproved(qg.isPassed());
        project.setQualityEvaluation(qg.getEvaluationSummary());

        Project saved = projectRepository.save(project);

        // Link technologies idempotently
        if (techs != null) {
            for (Technology t : techs) {
                boolean alreadyLinked = saved.getProjectTechnologies().stream()
                        .anyMatch(pt -> pt.getTechnology().getId().equals(t.getId()));
                if (!alreadyLinked) {
                    ProjectTechnology pt = new ProjectTechnology();
                    pt.setProject(saved);
                    pt.setTechnology(t);
                    saved.getProjectTechnologies().add(pt);
                }
            }
        }

        // Link requirements idempotently
        if (saved.getRequirements().isEmpty()) {
            ProjectRequirement r1 = new ProjectRequirement();
            r1.setProject(saved);
            r1.setRequirementText("Core Functional: " + functionalRequirements);
            saved.getRequirements().add(r1);

            ProjectRequirement r2 = new ProjectRequirement();
            r2.setProject(saved);
            r2.setRequirementText("Non-Functional & Reliability: " + nonFunctionalRequirements);
            saved.getRequirements().add(r2);
        }

        projectRepository.save(saved);
    }

    private ProjectDomain getDomain(Map<String, ProjectDomain> map, String... names) {
        for (String n : names) {
            ProjectDomain d = map.get(n.toLowerCase());
            if (d != null) return d;
        }
        return map.values().stream().findFirst().orElse(null);
    }

    private List<EngineeringBranch> getBranches(Map<String, EngineeringBranch> map, String... names) {
        List<EngineeringBranch> list = new ArrayList<>();
        for (String n : names) {
            EngineeringBranch b = map.get(n.toLowerCase());
            if (b != null) list.add(b);
        }
        return list;
    }

    private List<Skill> getSkills(Map<String, Skill> map, String... names) {
        List<Skill> list = new ArrayList<>();
        for (String n : names) {
            Skill s = map.get(n.toLowerCase());
            if (s != null) list.add(s);
        }
        return list;
    }

    private List<Technology> getTechs(Map<String, Technology> map, String... names) {
        List<Technology> list = new ArrayList<>();
        for (String n : names) {
            Technology t = map.get(n.toLowerCase());
            if (t != null) list.add(t);
        }
        return list;
    }
}
