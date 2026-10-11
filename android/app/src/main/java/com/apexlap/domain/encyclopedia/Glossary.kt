package com.apexlap.domain.encyclopedia

enum class GlossaryCategory(val label: String) {
    RACE("Course"),
    STRATEGY("Stratégie"),
    TECHNICAL("Technique"),
    FLAGS("Drapeaux & sécurité"),
    RULES("Règlement"),
}

data class GlossaryTerm(val term: String, val category: GlossaryCategory, val definition: String)

object Glossary {
    val terms: List<GlossaryTerm> = listOf(
        GlossaryTerm("Pole position", GlossaryCategory.RACE, "Première place sur la grille de départ, obtenue par le pilote le plus rapide des qualifications."),
        GlossaryTerm("Qualifications (Q1, Q2, Q3)", GlossaryCategory.RACE, "Séance qui fixe la grille. Les plus lents sont éliminés en Q1 puis en Q2 ; les dix meilleurs se disputent la pole en Q3."),
        GlossaryTerm("Sprint", GlossaryCategory.RACE, "Course courte (environ 100 km) disputée le samedi de certains week-ends, avec ses propres qualifications et des points pour les huit premiers."),
        GlossaryTerm("Tour de formation", GlossaryCategory.RACE, "Tour lancé avant le départ pour chauffer pneus et freins, puis reprendre sa place sur la grille."),
        GlossaryTerm("Meilleur tour en course", GlossaryCategory.RACE, "Tour le plus rapide de la course. Il a rapporté un point bonus de 2019 à 2024 (si le pilote finissait dans les dix)."),
        GlossaryTerm("Hat-trick / Grand Chelem", GlossaryCategory.RACE, "Hat-trick : pole, victoire et meilleur tour. Grand Chelem : la même chose en menant chaque tour de la course."),
        GlossaryTerm("Doublé", GlossaryCategory.RACE, "Victoire et deuxième place pour les deux voitures d'une même écurie."),
        GlossaryTerm("Tour de retard (lapped)", GlossaryCategory.RACE, "Une voiture prise d'un tour par le leader ; elle doit laisser passer les plus rapides sous drapeau bleu."),
        GlossaryTerm("Undercut", GlossaryCategory.STRATEGY, "S'arrêter au stand avant un rival pour profiter de pneus neufs plus rapides et ressortir devant lui après son propre arrêt."),
        GlossaryTerm("Overcut", GlossaryCategory.STRATEGY, "L'inverse de l'undercut : rester en piste plus longtemps, quand les pneus usés restent rapides ou que la piste se libère, pour passer devant à l'arrêt."),
        GlossaryTerm("Relais (stint)", GlossaryCategory.STRATEGY, "Période passée en piste entre deux arrêts sur un même train de pneus."),
        GlossaryTerm("Gommes (Soft, Medium, Hard)", GlossaryCategory.STRATEGY, "Les trois mélanges secs Pirelli d'un week-end : tendre (rouge, rapide mais fragile), medium (jaune) et dur (blanc, durable). En course sèche, deux mélanges différents sont obligatoires."),
        GlossaryTerm("Intermédiaires et pluie", GlossaryCategory.STRATEGY, "Pneus rainurés pour piste humide (intermédiaire, vert) ou détrempée (pluie extrême, bleu)."),
        GlossaryTerm("Dégradation", GlossaryCategory.STRATEGY, "Perte de performance des pneus au fil des tours, qui dicte la stratégie d'arrêts."),
        GlossaryTerm("Box, box", GlossaryCategory.STRATEGY, "Appel radio demandant au pilote de rentrer au stand ce tour-ci."),
        GlossaryTerm("Aspiration", GlossaryCategory.TECHNICAL, "Réduction de la traînée derrière une voiture, qui permet au poursuivant d'aller plus vite en ligne droite."),
        GlossaryTerm("DRS", GlossaryCategory.TECHNICAL, "Drag Reduction System : volet d'aileron arrière ouvrable dans des zones définies quand on est à moins d'une seconde du pilote devant. Remplacé en 2026 par l'aérodynamique active."),
        GlossaryTerm("Aérodynamique active", GlossaryCategory.TECHNICAL, "Depuis 2026, ailerons avant et arrière ajustables : mode faible traînée en ligne droite, appui maximal en virage."),
        GlossaryTerm("Appui (downforce)", GlossaryCategory.TECHNICAL, "Force aérodynamique qui plaque la voiture au sol et permet de passer les virages plus vite."),
        GlossaryTerm("Effet de sol", GlossaryCategory.TECHNICAL, "Appui généré par le dessous de la voiture grâce à des canaux (venturi) qui accélèrent l'air sous le plancher."),
        GlossaryTerm("ERS / MGU-K", GlossaryCategory.TECHNICAL, "Système de récupération d'énergie : le MGU-K récupère l'énergie au freinage et la restitue sous forme de puissance électrique. Sa part atteint près de 50 % en 2026."),
        GlossaryTerm("Unité de puissance", GlossaryCategory.TECHNICAL, "Le moteur hybride complet : V6 turbo 1,6 L, batterie, électronique et moteurs électriques."),
        GlossaryTerm("Télémétrie", GlossaryCategory.TECHNICAL, "Données transmises en temps réel par la voiture (vitesse, régime, freins, gaz…), comme celles du comparateur de tours."),
        GlossaryTerm("Mini-secteur", GlossaryCategory.TECHNICAL, "Subdivision d'un secteur de chronométrage, utile pour voir où un pilote gagne ou perd du temps."),
        GlossaryTerm("Drapeau jaune", GlossaryCategory.FLAGS, "Danger sur la piste : ralentir, interdiction de dépasser dans le secteur. Double jaune : être prêt à s'arrêter."),
        GlossaryTerm("Drapeau bleu", GlossaryCategory.FLAGS, "Un pilote plus rapide qui vous prend un tour arrive : il faut le laisser passer."),
        GlossaryTerm("Drapeau rouge", GlossaryCategory.FLAGS, "Séance interrompue : toutes les voitures rentrent dans la voie des stands."),
        GlossaryTerm("Drapeau à damier", GlossaryCategory.FLAGS, "Fin de la séance ou de la course."),
        GlossaryTerm("Safety Car", GlossaryCategory.FLAGS, "Voiture de sécurité qui neutralise la course : le peloton se regroupe derrière elle, sans dépassement."),
        GlossaryTerm("Virtual Safety Car (VSC)", GlossaryCategory.FLAGS, "Neutralisation sans voiture de sécurité : chaque pilote doit respecter un temps minimal par secteur, les écarts sont conservés."),
        GlossaryTerm("Parc fermé", GlossaryCategory.RULES, "Régime qui interdit de modifier les réglages de la voiture entre les qualifications et la course."),
        GlossaryTerm("Pénalité de temps", GlossaryCategory.RULES, "Secondes ajoutées au temps de course ou à purger lors d'un arrêt (5 s, 10 s), infligées par les commissaires."),
        GlossaryTerm("Pénalité de grille", GlossaryCategory.RULES, "Recul sur la grille de départ, par exemple après un changement d'éléments moteur au-delà du quota."),
        GlossaryTerm("Limites de piste", GlossaryCategory.RULES, "Les lignes blanches délimitent la piste ; les dépasser avec les quatre roues fait annuler le tour ou mène à une pénalité."),
        GlossaryTerm("Barème des points", GlossaryCategory.RULES, "En course : 25, 18, 15, 12, 10, 8, 6, 4, 2 et 1 point du 1er au 10e. En sprint : 8 à 1 point du 1er au 8e."),
        GlossaryTerm("Plafond budgétaire", GlossaryCategory.RULES, "Limite de dépenses annuelles imposée aux écuries depuis 2021 pour réduire les écarts de moyens."),
    )
}
