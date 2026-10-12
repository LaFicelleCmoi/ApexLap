package com.apexlap.domain.encyclopedia

data class KeyCorner(val name: String, val description: String)

data class CircuitGuide(val summary: String, val corners: List<KeyCorner>)

/** Hand-written notes on the key corners of the circuits of the current calendar, keyed by Jolpica circuit id. */
object CircuitGuides {
    fun of(circuitId: String): CircuitGuide? = guides[circuitId]

    private val guides: Map<String, CircuitGuide> = mapOf(
        "albert_park" to CircuitGuide(
            "Circuit semi-permanent autour du lac d'Albert Park, rapide et fluide depuis sa refonte de 2022.",
            listOf(
                KeyCorner("Virages 1-2", "Freinage serré juste après le départ, souvent le théâtre d'accrochages au premier tour."),
                KeyCorner("Virages 9-10", "Ancienne chicane remplacée en 2022 par un enchaînement très rapide gauche-droite."),
                KeyCorner("Virages 11-12", "Courbes à haute vitesse qui demandent une voiture stable."),
            ),
        ),
        "shanghai" to CircuitGuide(
            "Tracé dessiné sur le caractère chinois « shang », connu pour son premier virage en escargot et sa longue ligne droite.",
            listOf(
                KeyCorner("Virages 1-2", "Long droite qui se referme sur lui-même : la gestion du pneu avant gauche est cruciale."),
                KeyCorner("Virage 13", "Longue courbe à droite qui lance la ligne droite de plus d'un kilomètre."),
                KeyCorner("Virage 14", "Épingle au bout de la ligne droite, principale zone de dépassement."),
            ),
        ),
        "suzuka" to CircuitGuide(
            "Unique tracé en huit du calendrier, référence absolue pour les pilotes.",
            listOf(
                KeyCorner("Les S", "Enchaînement de courbes du premier secteur où le rythme se joue au centimètre."),
                KeyCorner("Degner", "Deux droites rapides et piégeuses, bordées de graviers."),
                KeyCorner("Spoon", "Long double gauche qui conditionne la vitesse dans la ligne droite arrière."),
                KeyCorner("130R", "Gauche à plus de 300 km/h, l'un des virages les plus célèbres au monde."),
                KeyCorner("Chicane Casio", "Dernière chicane, ultime chance de dépasser avant la ligne d'arrivée."),
            ),
        ),
        "bahrain" to CircuitGuide(
            "Circuit du désert de Sakhir, abrasif pour les pneus, disputé de nuit.",
            listOf(
                KeyCorner("Virage 1", "Gros freinage après la ligne droite des stands, meilleure zone de dépassement."),
                KeyCorner("Virage 4", "Droite en montée, deuxième occasion de dépasser dans le premier tour."),
                KeyCorner("Virages 9-10", "Freinage en courbe où l'on bloque facilement la roue avant."),
            ),
        ),
        "jeddah" to CircuitGuide(
            "Circuit urbain le long de la corniche, parmi les plus rapides du monde avec des murs très proches.",
            listOf(
                KeyCorner("Virages 1-2", "Chicane au bout de la ligne droite des stands, point chaud des relances."),
                KeyCorner("Virage 13", "Grand virage relevé, pris à pleine vitesse."),
                KeyCorner("Virage 27", "Dernière courbe qui lance vers la ligne d'arrivée."),
            ),
        ),
        "miami" to CircuitGuide(
            "Circuit tracé autour du Hard Rock Stadium, mélange de longues lignes droites et de sections lentes.",
            listOf(
                KeyCorner("Virage 1", "Premier freinage, large et propice aux attaques."),
                KeyCorner("Virages 14-15", "Chicane lente et étroite, très technique."),
                KeyCorner("Virage 17", "Épingle au bout de la longue ligne droite, zone de dépassement."),
            ),
        ),
        "imola" to CircuitGuide(
            "Autodromo Enzo e Dino Ferrari, tracé historique à l'ancienne, étroit et vallonné.",
            listOf(
                KeyCorner("Tamburello", "Ancienne courbe à fond devenue chicane après 1994."),
                KeyCorner("Tosa", "Épingle en montée, l'une des rares zones de dépassement."),
                KeyCorner("Acque Minerali", "Double droite en descente puis en montée, très exigeante."),
                KeyCorner("Rivazza", "Double gauche en descente avant la ligne droite des stands."),
            ),
        ),
        "monaco" to CircuitGuide(
            "Le plus lent et le plus prestigieux des circuits, où la qualification fait presque tout.",
            listOf(
                KeyCorner("Sainte-Dévote", "Premier virage à droite, souvent encombré au départ."),
                KeyCorner("Casino", "Droite en sommet de colline devant le casino de Monte-Carlo."),
                KeyCorner("Épingle du Fairmont", "Le virage le plus lent du championnat, à environ 50 km/h."),
                KeyCorner("Tunnel", "Seule section à pleine vitesse, en légère courbe et sans lumière naturelle."),
                KeyCorner("Piscine", "Chicane rapide où les pilotes frôlent les rails."),
            ),
        ),
        "catalunya" to CircuitGuide(
            "Circuit de référence pour les essais : celui qui va vite ici va vite partout.",
            listOf(
                KeyCorner("Virage 1", "Freinage au bout de la ligne droite des stands, meilleure zone de dépassement."),
                KeyCorner("Virage 3", "Longue courbe à droite qui met les pneus avant à rude épreuve."),
                KeyCorner("Virages 13-14", "Dernières courbes rapides, rendues à leur tracé d'origine en 2023 avec la suppression de la chicane."),
            ),
        ),
        "villeneuve" to CircuitGuide(
            "Circuit Gilles-Villeneuve sur l'île Notre-Dame, fait de freinages violents et de murs proches.",
            listOf(
                KeyCorner("Virages 1-2", "Gauche-droite serré juste après le départ."),
                KeyCorner("Épingle", "Virage le plus lent, point de départ de la longue ligne droite."),
                KeyCorner("Mur des champions", "Sortie de la dernière chicane où de nombreux champions ont fini dans le mur."),
            ),
        ),
        "red_bull_ring" to CircuitGuide(
            "Tour très court et vallonné dans les montagnes de Styrie, avec des temps au tour très serrés.",
            listOf(
                KeyCorner("Virage 1", "Droite en montée qui ouvre la première ligne droite."),
                KeyCorner("Virage 3", "Épingle en haut de la colline, meilleure zone de dépassement."),
                KeyCorner("Virage 4", "Freinage en descente, deuxième occasion d'attaquer."),
                KeyCorner("Virages 9-10", "Deux droites rapides en descente vers la ligne d'arrivée."),
            ),
        ),
        "silverstone" to CircuitGuide(
            "Berceau du championnat du monde (1950), ancien aérodrome très rapide.",
            listOf(
                KeyCorner("Copse", "Droite prise à très haute vitesse après la ligne droite."),
                KeyCorner("Maggotts-Becketts-Chapel", "Enchaînement de virages rapides, juge de paix aérodynamique."),
                KeyCorner("Stowe", "Freinage au bout de la ligne droite du Hangar, zone de dépassement."),
                KeyCorner("Club", "Derniers virages vers la ligne d'arrivée."),
            ),
        ),
        "spa" to CircuitGuide(
            "Spa-Francorchamps, le plus long tour du calendrier, dans les Ardennes et sa météo changeante.",
            listOf(
                KeyCorner("La Source", "Épingle du départ, piège classique du premier tour."),
                KeyCorner("Eau Rouge / Raidillon", "Compression puis montée aveugle à pleine vitesse, virage mythique."),
                KeyCorner("Pouhon", "Double gauche rapide en descente."),
                KeyCorner("Blanchimont", "Gauche à fond avant la dernière chicane."),
                KeyCorner("Arrêt de bus", "Chicane finale, dernière occasion de dépasser."),
            ),
        ),
        "hungaroring" to CircuitGuide(
            "Circuit sinueux et poussiéreux près de Budapest, souvent comparé à un Monaco sans murs.",
            listOf(
                KeyCorner("Virage 1", "Freinage en descente, principale zone de dépassement."),
                KeyCorner("Virage 4", "Gauche rapide et aveugle en sortie de bosse."),
                KeyCorner("Chicane 6-7", "Droite-gauche lent où l'on peut tenter un dépassement."),
            ),
        ),
        "zandvoort" to CircuitGuide(
            "Circuit des dunes, étroit et vallonné, célèbre pour ses virages relevés.",
            listOf(
                KeyCorner("Tarzan", "Virage 1 en épingle, quasiment la seule vraie zone de dépassement."),
                KeyCorner("Hugenholtz", "Virage 3 relevé à près de 19 degrés."),
                KeyCorner("Scheivlak", "Droite rapide et aveugle en descente."),
                KeyCorner("Arie Luyendyk", "Dernier virage relevé, pris à pleine vitesse."),
            ),
        ),
        "monza" to CircuitGuide(
            "Le Temple de la vitesse, tracé à très faible appui où les moteurs tournent à fond la majeure partie du tour.",
            listOf(
                KeyCorner("Variante del Rettifilo", "Première chicane après la ligne droite des stands, freinage le plus fort."),
                KeyCorner("Lesmo", "Deux droites rapides qui demandent de la précision."),
                KeyCorner("Ascari", "Chicane rapide gauche-droite-gauche."),
                KeyCorner("Parabolica", "Longue courbe finale (Curva Alboreto) qui lance la ligne droite."),
            ),
        ),
        "baku" to CircuitGuide(
            "Circuit urbain contrasté entre la vieille ville étroite et une ligne droite de plus de 2 km.",
            listOf(
                KeyCorner("Virage 1", "Gauche à angle droit, premier freinage majeur."),
                KeyCorner("Section du château", "Passage très étroit le long des remparts de la vieille ville."),
                KeyCorner("Ligne droite des stands", "Plus de 2 km pied au plancher, aspiration décisive."),
            ),
        ),
        "marina_bay" to CircuitGuide(
            "Course de nuit à Singapour, chaude, humide et bosselée, longtemps la plus éprouvante physiquement.",
            listOf(
                KeyCorner("Virages 1-3", "Enchaînement serré à la sortie des stands, zone de dépassement au départ."),
                KeyCorner("Virage 7", "Gros freinage au bout de Raffles Boulevard."),
                KeyCorner("Section de la baie", "Ancienne partie lente remplacée en 2023 par une ligne droite plus rapide."),
            ),
        ),
        "americas" to CircuitGuide(
            "Circuit of the Americas à Austin, inspiré des grands virages des circuits européens.",
            listOf(
                KeyCorner("Virage 1", "Épingle à gauche au sommet d'une montée très raide."),
                KeyCorner("Les S (3-6)", "Enchaînement rapide inspiré de Maggotts-Becketts."),
                KeyCorner("Virage 12", "Gros freinage au bout de la longue ligne droite arrière."),
                KeyCorner("Virages 16-18", "Droite à plusieurs points de corde, clin d'œil au virage 8 d'Istanbul."),
            ),
        ),
        "rodriguez" to CircuitGuide(
            "Autódromo Hermanos Rodríguez, à plus de 2 200 m d'altitude : air raréfié et faible appui.",
            listOf(
                KeyCorner("Virage 1", "Freinage au bout d'une ligne droite de plus d'un kilomètre."),
                KeyCorner("Les S", "Enchaînement rapide du deuxième secteur."),
                KeyCorner("Foro Sol", "Passage dans le stade, ambiance unique au monde."),
            ),
        ),
        "interlagos" to CircuitGuide(
            "Circuit José Carlos Pace à São Paulo, tourné à l'envers et vallonné, souvent théâtre de courses folles.",
            listOf(
                KeyCorner("S do Senna", "Gauche-droite en descente juste après le départ."),
                KeyCorner("Descida do Lago", "Freinage au bout de la ligne droite opposée."),
                KeyCorner("Junção", "Dernier virage lent qui commande la longue montée vers la ligne."),
            ),
        ),
        "vegas" to CircuitGuide(
            "Course de nuit dans les rues de Las Vegas, températures fraîches et très longues lignes droites.",
            listOf(
                KeyCorner("Virage 1", "Gauche serré au premier freinage."),
                KeyCorner("Section de la Sphere", "Courbes autour de la Sphere avant les lignes droites."),
                KeyCorner("Le Strip", "Ligne droite sur le célèbre boulevard, terminée par une chicane."),
            ),
        ),
        "losail" to CircuitGuide(
            "Circuit de Losail au Qatar, construit pour la moto, rapide et fluide, disputé de nuit.",
            listOf(
                KeyCorner("Virage 1", "Freinage au bout de la longue ligne droite des stands."),
                KeyCorner("Virages 12-14", "Enchaînement de droites rapides, très exigeant pour les pneus et la nuque."),
                KeyCorner("Virage 16", "Dernière droite rapide vers la ligne d'arrivée."),
            ),
        ),
        "yas_marina" to CircuitGuide(
            "Circuit de Yas Marina à Abou Dabi, traditionnelle finale de saison au crépuscule.",
            listOf(
                KeyCorner("Virage 5", "Épingle au bout de la première ligne droite, refaite en 2021."),
                KeyCorner("Virage 9", "Gros freinage au bout de la longue ligne droite arrière."),
                KeyCorner("Section de l'hôtel", "Virages lents sous le Yas Hotel."),
            ),
        ),
    )
}
