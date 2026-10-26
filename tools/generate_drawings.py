"""Author 200 original, region-based vector coloring sheets. No runtime downloads.

The small drawing vocabulary is shared, but each subject has its own silhouette,
parts and line work. Generated JSON is committed; normal Android builds do not
run this script. SVG contact sheets can be rendered for authoring QA.
"""
from pathlib import Path
import copy
import hashlib
import json
import math
import re

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/assets/drawings"
INK = "#243D59"
COLORS = ["#8ACB9B", "#FFCD68", "#F8AA91", "#8DCDE5", "#BAA2E0", "#F4A2BD"]


def path(d, color="#8ACB9B", region=True, width=1.3, ink=False):
    return {"d": d, "color": color, "region": region, "width": width, "ink": ink}


def oval(x, y, rx, ry=None, color="#8ACB9B", region=True, ink=False):
    ry = rx if ry is None else ry
    k = .55228475
    return path(f"M {x-rx} {y} C {x-rx} {y-k*ry} {x-k*rx} {y-ry} {x} {y-ry} C {x+k*rx} {y-ry} {x+rx} {y-k*ry} {x+rx} {y} C {x+rx} {y+k*ry} {x+k*rx} {y+ry} {x} {y+ry} C {x-k*rx} {y+ry} {x-rx} {y+k*ry} {x-rx} {y} Z", color, region, ink=ink)


def box(x, y, w, h, color="#8DCDE5", r=3):
    return path(f"M {x+r} {y} L {x+w-r} {y} Q {x+w} {y} {x+w} {y+r} L {x+w} {y+h-r} Q {x+w} {y+h} {x+w-r} {y+h} L {x+r} {y+h} Q {x} {y+h} {x} {y+h-r} L {x} {y+r} Q {x} {y} {x+r} {y} Z", color)


def poly(points, color="#FFCD68"):
    return path("M " + " L ".join(f"{x} {y}" for x, y in points) + " Z", color)


def line(d, width=1.2):
    return path(d, INK, False, width)


def transform(shapes, x=0, y=0, sx=1, sy=None):
    sy = sx if sy is None else sy
    result = []
    for shape in copy.deepcopy(shapes):
        tokens = re.findall(r"[MLQCZ]|-?(?:\d*\.)?\d+(?:e[-+]?\d+)?", shape["d"], re.I)
        axis = 0
        out = []
        for token in tokens:
            if token.upper() in "MLQCZ":
                out.append(token.upper()); axis = 0
            else:
                value = float(token) * (sx if axis % 2 == 0 else sy) + (x if axis % 2 == 0 else y)
                out.append(f"{value:.3f}".rstrip("0").rstrip(".")); axis += 1
        shape["d"] = " ".join(out)
        shape["width"] *= min(abs(sx), abs(sy))
        result.append(shape)
    return result


def face(x=50, y=43, size=1):
    return transform([
        oval(-7, 0, 1.8, 2.5, INK, False, True),
        oval(7, 0, 1.8, 2.5, INK, False, True),
        line("M -5 9 Q 0 14 5 9", 1.5),
    ], x, y, size)


def star(cx=50, cy=50, radius=32, color="#FFCD68", points=5):
    return poly([(cx + (radius if i % 2 == 0 else radius*.47)*math.cos(-math.pi/2+i*math.pi/points), cy + (radius if i % 2 == 0 else radius*.47)*math.sin(-math.pi/2+i*math.pi/points)) for i in range(points*2)], color)


def leaf(x=50, y=50, size=25):
    return [path(f"M {x-size} {y+size*.6} Q {x-size} {y-size} {x+size} {y-size*.6} Q {x+size} {y+size} {x-size} {y+size*.6} Z"), line(f"M {x-size} {y+size*.6} L {x+size*.7} {y-size*.4}")]


def cloud(x=50, y=50, size=1):
    return transform([path("M 18 62 C 1 62 1 37 20 37 C 19 15 49 10 58 30 C 79 17 94 37 85 48 C 106 63 84 73 68 70 L 25 70 Q 17 70 18 62 Z", "#B9E3F0")], x-50*size, y-50*size, size)


def flower(kind="flower"):
    result = [box(47, 44, 6, 46, "#8ACB9B"), *leaf(34, 72, 14), *leaf(64, 80, 12)]
    if kind == "tulip":
        result += [path("M 24 25 L 40 38 L 50 17 L 60 38 L 77 25 L 72 52 Q 50 76 28 52 Z", "#F4A2BD")]
    elif kind == "rose":
        result += [path("M 50 16 C 76 8 89 32 74 47 C 89 68 57 77 45 64 C 20 74 9 44 29 34 C 25 16 40 10 50 16 Z", "#F8AA91"), path("M 50 25 C 72 17 74 48 52 47 C 34 57 29 31 50 25 Z", "#F4A2BD"), line("M 48 34 Q 59 28 60 39")]
    else:
        count = 12 if kind == "sunflower" else 6
        for i in range(count):
            a=i*2*math.pi/count
            result.append(oval(50+23*math.cos(a), 39+23*math.sin(a), 10 if count==12 else 15, 14, "#FFCD68" if count==12 else "#F4A2BD"))
        result.append(oval(50,39,17,color="#D9AD79" if count==12 else "#FFCD68"))
        result += face(50,36,.65)
    return result


def animal(kind):
    c={"pig":"#F4A2BD","cow":"#F9F4DD","cat":"#F5BB73","dog":"#C99A73","lion":"#FFCD68","elephant":"#A9C2D3","frog":"#8ACB9B","monkey":"#C79976","owl":"#C1A6D9","penguin":"#98B8C9","fox":"#F8AA70","panda":"#F8F5E9","tiger":"#F8B565","zebra":"#F7F4E9","hippo":"#C4A5D9","rhino":"#ACBDCA"}.get(kind,"#AED4B0")
    if kind in ["hippo","rhino"]:
        r=[box(19,69,15,24,c),box(61,69,15,24,c),oval(45,61,36,24,c),oval(75,43,22,23,c),oval(64,19,6,8,c),oval(84,20,6,8,c),oval(81,50,18,13,c),oval(72,36,2.5,3,INK,False,True),line("M 84 55 Q 92 59 97 53")]
        if kind=="hippo":r += [oval(88,46,2,2,INK,False,True),box(82,61,5,7,"#F9F4DD")]
        else:r += [poly([(86,41),(92,18),(98,45)],"#F9F4DD")]
        return r
    if kind=="tiger":
        return [path("M 71 73 C 109 86 104 19 85 24 Q 77 28 84 38 Q 88 41 90 36 C 98 47 92 80 72 66 Z",c),oval(50,73,25,22,c),oval(25,23,13,color=c),oval(75,23,13,color=c),oval(50,42,32,28,c),oval(32,88,14,8,c),oval(68,88,14,8,c),oval(50,75,15,15,"#F9E2B3"),poly([(43,15),(50,31),(57,15)],"#C99A73"),poly([(19,33),(33,39),(19,43)],"#C99A73"),poly([(81,33),(67,39),(81,43)],"#C99A73"),*face(50,40,1.1),oval(50,50,4,3,INK,False,True)]
    if kind in ["fish","dolphin","whale","shark","seahorse"]:
        if kind=="seahorse":
            return [path("M 55 15 C 78 12 83 32 70 44 L 83 49 L 79 60 L 65 55 Q 57 70 65 75 C 85 90 49 100 44 81 Q 38 65 47 55 L 35 57 L 39 43 Q 27 24 42 18 Z",c), poly([(42,47),(25,39),(28,65),(43,61)],"#F4A2BD"), oval(62,29,2,3,INK,False,True), line("M 53 76 C 75 80 58 91 54 81")]
        if kind=="whale":
            return [path("M 12 52 C 12 18 62 17 72 48 Q 83 57 88 37 L 98 31 L 94 53 L 85 60 C 76 94 14 90 12 52 Z","#8DCDE5"), path("M 23 63 Q 44 78 66 60 Q 70 89 37 84 Z","#DBEDF2"), poly([(44,61),(35,83),(57,72)],"#9FBFE6"), oval(30,49,2.5,3,INK,False,True), line("M 17 59 Q 22 66 28 62"), path("M 39 20 Q 25 6 30 3 Q 41 1 43 14 Q 46 1 55 3 Q 61 8 48 21 Z","#8DCDE5")]
        body=path("M 13 50 Q 28 12 68 32 Q 79 37 83 45 L 98 31 L 94 64 L 82 54 Q 54 91 20 63 Z", "#8DCDE5")
        if kind=="fish":
            return [poly([(20,50),(2,26),(2,76)],"#FFCD68"),oval(52,49,38,25,"#8DCDE5"),poly([(40,28),(48,10),(65,29)],"#F4A2BD"),oval(47,57,13,10,"#FFCD68"),oval(74,42,2.8,3.5,INK,False,True),line("M 77 57 Q 84 61 88 55")]
        return [poly([(45,35),(55,12),(61,35)],"#92BDD8"),body,poly([(50,60),(42,79),(65,65)],"#A2C7DD"),oval(29,44,2.4,3,INK,False,True),line("M 18 55 Q 29 60 39 54"),*([line("M 35 53 L 36 60 L 42 55 L 44 61 L 49 54")] if kind=="shark" else [])]
    if kind in ["butterfly","bee","ladybug","snail","hedgehog","crab","octopus","turtle"]:
        if kind=="butterfly":
            r=[oval(27,34,22,26,"#F4A2BD"),oval(73,34,22,26,"#F4A2BD"),oval(29,73,20,18,"#BAA2E0"),oval(71,73,20,18,"#BAA2E0"),oval(50,55,7,30,"#FFCD68"),oval(50,24,9,10,"#FFCD68"),line("M 46 16 Q 29 0 32 9 M 54 16 Q 71 0 68 9")]
            return r+face(50,24,.38)+[oval(25,34,9,12,"#FFCD68"),oval(75,34,9,12,"#FFCD68")]
        if kind=="bee":
            return [oval(39,26,20,19,"#DBEDF2"),oval(64,25,18,19,"#DBEDF2"),oval(51,56,37,26,"#FFCD68"),path("M 42 31 Q 34 56 42 81 L 53 82 Q 45 56 53 31 Z","#C99A73"),path("M 62 33 Q 54 57 62 79 L 72 74 Q 65 57 72 37 Z","#C99A73"),oval(23,51,3,4,INK,False,True),line("M 15 61 Q 21 69 27 62 M 26 32 L 18 19 M 15 38 L 8 30")]
        if kind=="ladybug":
            r=[line("M 27 36 L 12 27 M 23 53 L 6 55 M 26 72 L 13 83 M 73 36 L 88 27 M 77 53 L 94 55 M 74 72 L 87 83",2),oval(50,25,20,17,"#AEBBCA"),oval(50,57,32,34,"#F8AA91"),line("M 50 26 L 50 90")]
            for x,y in [(34,42),(66,42),(30,62),(70,62),(39,78),(61,78)]:r.append(oval(x,y,6,color="#BAA2E0"))
            return r+face(50,20,.65)
        if kind=="snail":
            return [path("M 10 75 Q 25 62 49 71 L 72 70 L 72 43 Q 82 28 90 44 L 91 76 Q 68 94 13 86 Z","#AED4B0"),oval(39,52,29,29,"#F5BB73"),line("M 42 72 C 8 60 25 17 52 35 C 79 59 30 78 31 47 C 34 35 50 46 42 54"),line("M 77 37 L 72 22 M 86 37 L 92 22"),*face(82,45,.45)]
        if kind=="hedgehog":
            return [poly([(9,75),(14,58),(9,44),(23,43),(28,23),(39,32),(49,14),(57,30),(74,22),(77,41),(92,47),(82,61),(92,76)],"#C99A73"),path("M 24 71 Q 28 42 59 47 Q 80 53 84 76 Q 59 91 24 80 Z","#F2D6B3"),oval(65,64,2,3,INK,False,True),oval(85,73,4,color=INK,region=False,ink=True),oval(35,84,8,5,"#F2D6B3"),oval(66,85,8,5,"#F2D6B3")]
        if kind=="crab":
            return [line("M 25 61 L 7 64 L 2 74 M 25 72 L 9 79 L 5 89 M 75 61 L 93 64 L 98 74 M 75 72 L 91 79 L 95 89",2),poly([(25,56),(12,38),(4,28),(7,11),(19,23),(28,11),(31,32)],"#F8AA91"),poly([(75,56),(88,38),(96,28),(93,11),(81,23),(72,11),(69,32)],"#F8AA91"),oval(50,64,32,22,"#F8AA91"),oval(39,40,7,10,"#FFCD68"),oval(61,40,7,10,"#FFCD68"),*face(50,41,.7)]
        if kind=="octopus":
            return [path("M 23 55 C 2 1 98 1 77 55 C 97 65 100 91 86 91 Q 77 91 77 74 Q 76 96 65 94 Q 56 93 58 76 Q 51 98 43 94 Q 36 91 38 75 Q 27 96 18 89 Q 9 82 23 55 Z","#BAA2E0"),*face(50,37,1.3),oval(29,51,6,3,"#F4A2BD"),oval(71,51,6,3,"#F4A2BD")]
        return [oval(25,75,11,9,"#8ACB9B"),oval(61,77,11,9,"#8ACB9B"),poly([(20,62),(4,52),(8,72)],"#8ACB9B"),oval(47,53,34,28,"#FFCD68"),oval(83,54,14,16,"#8ACB9B"),poly([(47,30),(66,43),(60,65),(36,65),(28,44)],"#8ACB9B"),line("M 28 44 L 16 40 M 36 65 L 30 78 M 60 65 L 69 73 M 66 43 L 73 39"),oval(87,49,2,3,INK,False,True),line("M 84 62 Q 90 65 94 59")]
    if kind in ["bird","duck","hen","owl","penguin"]:
        r=[]
        if kind=="owl": r += [poly([(25,31),(22,7),(43,24)],c),poly([(75,31),(78,7),(57,24)],c)]
        if kind=="hen":r += [oval(43,16,7,10,"#F8AA91"),oval(53,12,7,10,"#F8AA91"),oval(62,18,7,10,"#F8AA91")]
        r += [poly([(29,74),(29,91),(17,91),(29,87),(39,91)],"#FFCD68"),poly([(70,74),(70,91),(83,91),(70,87),(60,91)],"#FFCD68"),oval(50,52,32,35,c if kind!="duck" else "#FFCD68"),oval(50,65,21,20,"#F9F4DD"),oval(22,58,10,20,c),oval(78,58,10,20,c)]
        if kind=="owl":r += [oval(36,40,13,15,"#F9F4DD"),oval(64,40,13,15,"#F9F4DD")]
        r += face(50,38,1.1)+[poly([(43,48),(57,48),(50,58)],"#F5BB73")]
        if kind=="duck":r[-1]=box(39,46,27,12,"#F5BB73",6)
        if kind=="bird":r += [path("M 47 20 Q 34 1 40 4 L 53 19 Q 63 0 69 6 L 58 22 Z","#8DCDE5")]
        return r
    if kind=="frog":
        return [oval(24,76,18,12,c),oval(76,76,18,12,c),oval(50,64,30,26,c),oval(50,35,37,25,c),oval(30,18,12,13,c),oval(70,18,12,13,c),oval(50,68,18,17,"#DBEAB7"),oval(30,18,3,4,INK,False,True),oval(70,18,3,4,INK,False,True),line("M 32 39 Q 50 56 68 39",2),line("M 16 86 L 9 90 M 24 86 L 23 93 M 76 86 L 77 93 M 84 86 L 91 90")]
    if kind=="giraffe":
        return [box(21,66,10,28,"#FFCD68"),box(60,66,10,28,"#FFCD68"),oval(47,65,31,17,"#FFCD68"),box(59,25,15,48,"#FFCD68"),line("M 21 58 Q 3 50 6 37",2),oval(68,24,17,12,"#FFCD68"),poly([(56,15),(45,9),(48,23)],"#FFCD68"),line("M 64 14 L 62 5 M 75 13 L 78 4",2),oval(62,5,3,color="#C99A73"),oval(78,4,3,color="#C99A73"),oval(76,21,2,2.5,INK,False,True),oval(31,63,5,6,"#C99A73"),oval(48,70,6,4,"#C99A73"),oval(65,46,4,6,"#C99A73"),oval(65,62,4,5,"#C99A73")]
    if kind in ["crocodile","horse","zebra","goat","sheep","cow","hippo","rhino","tiger"]:
        r=[line("M 22 54 Q 5 45 9 32",2),box(22,67,10,26,c),box(61,67,10,26,c),oval(47,59,33,23,c)]
        if kind=="crocodile":
            r=[poly([(18,58),(3,51),(10,66),(5,78),(28,72)],c),*r[1:],path("M 56 48 L 91 48 Q 101 60 91 72 L 61 73 Z",c),oval(71,46,8,9,c),oval(73,44,2,3,INK,False,True),line("M 72 63 L 93 63")]
            for x in range(22,66,10):r.append(poly([(x,40),(x+5,28),(x+10,40)],"#FFCD68"))
            return r
        head=path("M 61 19 Q 74 4 85 22 L 83 37 Q 101 43 91 58 Q 75 68 63 48 L 55 43 Z",c)
        r.append(head)
        r += [poly([(65,21),(62,5),(72,17)],c),poly([(80,19),(85,6),(88,25)],c),oval(82,32,2.1,3,INK,False,True),line("M 83 51 Q 89 54 93 48")]
        if kind in ["goat","cow"]:r += [poly([(65,16),(58,1),(70,12)],"#D9AD79"),poly([(80,14),(87,1),(86,24)],"#D9AD79")]
        if kind=="goat":r += [poly([(78,59),(84,74),(89,59)],"#F9F4DD")]
        if kind=="cow":r += [oval(31,51,8,6,"#A3B0BE"),oval(52,63,6,9,"#A3B0BE"),oval(85,47,10,8,"#F4A2BD")]
        if kind=="sheep":
            r=r[:3]+[path("M 17 51 C 6 43 15 31 25 34 C 22 21 41 18 45 30 C 52 17 69 25 66 37 C 81 33 90 47 77 56 C 88 68 73 79 64 73 C 57 86 40 82 37 72 C 22 83 10 67 17 59 Z","#F9F4DD"),*r[4:]]
        if kind in ["horse","zebra"]:r += [path("M 60 19 L 56 9 L 69 12 L 65 24 L 62 42 L 56 53 L 51 39 Z","#B68C6A")]
        if kind in ["zebra","tiger"]:
            for x in [25,38,51]:r += [poly([(x,39),(x+8,38),(x+5,63),(x,57)],"#8494A8")]
        if kind=="rhino":r += [poly([(87,40),(96,22),(99,44)],"#F9F4DD")]
        if kind=="hippo":r += [oval(84,43,13,13,c),oval(90,41,2,2,INK,False,True)]
        return r
    # Distinct sitting animal silhouettes, with species-specific ears, tails and faces.
    r=[]
    if kind=="elephant":
        return [oval(50,66,30,25,c),box(24,72,16,21,c),box(60,72,16,21,c),oval(22,38,19,25,c),oval(78,38,19,25,c),oval(50,36,28,26,c),path("M 45 43 Q 65 43 58 68 Q 52 83 69 76 L 73 85 C 43 106 34 77 43 60 Z",c),oval(38,32,2,3,INK,False,True),oval(62,32,2,3,INK,False,True)]
    if kind=="lion":
        r += [path("M 50 8 L 62 15 L 75 11 L 82 24 L 94 31 L 89 46 L 94 59 L 82 69 L 75 82 L 60 80 L 50 88 L 36 80 L 22 81 L 17 67 L 7 57 L 13 43 L 9 29 L 23 24 L 30 11 L 43 15 Z","#D9AD79")]
    if kind=="rabbit":r += [oval(33,17,11,22,"#D9C4DE"),oval(66,17,11,22,"#D9C4DE"),oval(33,17,5,16,"#F4A2BD"),oval(66,17,5,16,"#F4A2BD")]
    elif kind in ["cat","fox","pig"]:r += [poly([(20,39),(17,5),(42,26)],c),poly([(80,39),(83,5),(58,26)],c)]
    elif kind=="dog":r += [oval(20,35,13,24,"#B68C6A"),oval(80,35,13,24,"#B68C6A")]
    else:r += [oval(23,24,14,14,c),oval(77,24,14,14,c)]
    if kind=="monkey":r += [path("M 72 67 C 101 46 98 10 88 16 Q 81 21 89 26 L 95 27 Q 89 36 81 27 C 64 4 106 -1 104 28 Q 104 59 78 76 Z", "#C79976")]
    if kind=="fox":r += [path("M 70 70 Q 107 31 98 82 Q 83 99 65 87 Z",c),path("M 85 58 Q 105 37 98 79 L 87 84 Z","#F9F4DD")]
    r += [oval(50,72,25,23,c),oval(50,42,31,28,c),oval(32,88,14,8,c),oval(68,88,14,8,c),oval(50,74,15,16,"#F6E6CA")]
    if kind=="pig":r += [oval(36,35,2.5,3.5,INK,False,True),oval(64,35,2.5,3.5,INK,False,True),oval(50,51,17,12,"#F4A2BD"),oval(44,50,2,3,INK,False,True),oval(56,50,2,3,INK,False,True)]
    elif kind=="monkey":r += [path("M 50 33 C 22 13 15 65 50 65 C 85 65 78 13 50 33 Z","#F6E6CA"),*face(50,40,.9)]
    else:
        if kind=="panda":r += [oval(36,39,8,10,"#9BAABD"),oval(64,39,8,10,"#9BAABD")]
        r += face(50,38,1.1)+[oval(50,48,4,3,INK,False,True)]
    if kind=="cat":r += [line("M 26 50 L 7 46 M 26 55 L 7 58 M 74 50 L 93 46 M 74 55 L 93 58")]
    if kind=="koala":r += [oval(50,45,7,10,"#8494A8")]
    return r


def dinosaur(kind):
    longnecks={"brontosaurus","diplodocus","brachiosaurus","apatosaurus","argentinosaurus"}
    flyers={"pteranodon","pterodactyl","quetzalcoatlus"}
    horns={"triceratops","styracosaurus","protoceratops","centrosaurus"}
    plates={"stegosaurus","kentrosaurus","ankylosaurus","euoplocephalus"}
    if kind in flyers:
        return [poly([(47,40),(12,16),(3,61),(30,53),(43,70)],"#BAA2E0"),poly([(53,40),(88,16),(97,61),(70,53),(57,70)],"#BAA2E0"),oval(50,52,9,28,"#8ACB9B"),poly([(48,26),(54,8),(73,27)],"#FFCD68"),oval(52,28,12,11,"#8ACB9B"),poly([(62,25),(82,30),(62,34)],"#FFCD68"),oval(57,24,2,2,INK,False,True),line("M 45 77 L 39 90 M 55 77 L 61 90"),*([poly([(47,24),(34,7),(54,15)],"#F4A2BD")] if kind=="quetzalcoatlus" else [])]
    if kind in longnecks:
        neck={"brontosaurus":20,"diplodocus":14,"brachiosaurus":8,"apatosaurus":24,"argentinosaurus":4}[kind]
        return [path("M 30 59 Q 10 53 3 37 Q 3 75 35 79 Z","#8ACB9B"),box(32,70,12,24,"#8ACB9B"),box(68,70,12,24,"#8ACB9B"),path(f"M 22 57 Q 39 34 62 53 L 65 {neck+13} Q 65 {neck-3} 81 {neck} Q 98 {neck+2} 95 {neck+15} Q 94 {neck+24} 81 {neck+22} L 82 61 Q 94 86 59 85 Q 22 87 22 57 Z","#8ACB9B"),oval(83,neck+10,2,2.5,INK,False,True),line(f"M 86 {neck+17} Q 91 {neck+20} 94 {neck+16}"),oval(44,61,5,7,"#FFCD68"),oval(60,70,6,5,"#FFCD68")]
    r=[path("M 35 65 Q 9 69 3 51 Q 0 83 39 83 Z","#8ACB9B"),box(30,69,13,25,"#8ACB9B"),box(62,69,13,25,"#8ACB9B")]
    if kind in plates:
        for i in range(5):r.append(poly([(19+i*12,47),(24+i*12,20-(i%2)*6),(35+i*12,46)],"#FFCD68"))
    if kind=="spinosaurus":r.append(path("M 22 52 Q 22 3 38 21 Q 48 -2 59 20 Q 75 4 77 51 Z","#F4A2BD"))
    r += [oval(50,59,35,24,"#8ACB9B")]
    if kind in horns:
        r += [path("M 63 28 L 64 10 L 77 17 L 91 11 L 95 27 L 99 40 L 89 51 L 70 48 Z","#BAA2E0"),oval(80,43,19,17,"#8ACB9B"),poly([(78,32),(76,11),(85,31)],"#FFCD68"),poly([(91,40),(99,29),(99,44)],"#FFCD68"),oval(84,38,2.2,2.5,INK,False,True),line("M 85 52 Q 92 57 97 49")]
        if kind=="styracosaurus":r += [poly([(68,22),(57,3),(76,17)],"#FFCD68"),poly([(87,19),(98,2),(96,28)],"#FFCD68")]
    elif kind in plates:
        r += [oval(85,58,13,13,"#8ACB9B"),oval(89,55,2,2.5,INK,False,True),line("M 87 65 Q 92 69 96 62")]
        if kind in {"ankylosaurus","euoplocephalus"}:r += [oval(9,64,8,9,"#FFCD68"),oval(33,50,6,5,"#BAA2E0"),oval(51,44,7,5,"#BAA2E0"),oval(68,51,6,5,"#BAA2E0")]
    else:
        r += [path("M 57 58 L 61 26 Q 78 8 94 26 L 96 42 Q 90 54 73 48 L 74 67 Z","#8ACB9B"),oval(81,29,2.7,3.4,INK,False,True),line("M 77 42 Q 86 47 95 40"),path("M 61 56 L 78 60 L 76 67 L 60 65 Z","#8ACB9B")]
        if kind in {"tyrannosaurus","allosaurus","carnotaurus"}:r += [poly([(80,44),(83,50),(86,45)],"#F9F4DD")]
        if kind=="parasaurolophus":r += [path("M 69 23 Q 39 9 49 1 L 81 15 Z","#FFCD68")]
        if kind=="carnotaurus":r += [poly([(73,22),(71,8),(82,21)],"#FFCD68")]
        if kind=="pachycephalosaurus":r += [oval(80,18,14,10,"#BAA2E0")]
        if kind=="dilophosaurus":r += [poly([(68,23),(65,5),(77,19)],"#F4A2BD"),poly([(82,19),(87,3),(91,23)],"#F4A2BD")]
        if kind=="iguanodon":r += [poly([(71,62),(83,50),(81,65)],"#FFCD68")]
    return r


def wheel(x,y,r=10):
    return [oval(x,y,r,color="#A7B9CC"),oval(x,y,r*.42,color="#F9F4DD")]


def vehicle(kind):
    if kind in ["bicycle","tricycle","scooter","motorbike"]:
        r=[*wheel(21,72,18),*wheel(79,72,18)]
        if kind=="scooter":return r+[box(22,69,48,7,"#F8AA91"),poly([(67,69),(71,21),(77,21),(75,73)],"#8DCDE5"),line("M 66 20 L 86 20",4)]
        r += [poly([(22,72),(41,37),(59,72)],"#8DCDE5"),poly([(42,37),(65,37),(79,72),(58,72)],"#FFCD68"),line("M 39 32 L 33 21 M 27 21 L 46 21 M 65 37 L 69 20 L 81 16",3)]
        if kind=="motorbike":r += [box(35,33,30,18,"#F8AA91"),box(25,27,20,6,"#BAA2E0")]
        if kind=="tricycle":r += wheel(44,81,10)
        return r
    if kind in ["airplane","spaceshuttle"]:
        r=[poly([(47,14),(15,65),(42,59),(44,89),(55,89),(58,59),(86,66),(56,14)],"#8DCDE5"),path("M 43 17 Q 50 0 57 17 L 58 76 Q 50 94 42 76 Z","#F9F4DD"),box(45,24,10,16,"#BAA2E0"),poly([(43,65),(27,88),(46,80)],"#F8AA91"),poly([(57,65),(73,88),(54,80)],"#F8AA91")]
        if kind=="spaceshuttle":r += [poly([(46,87),(50,99),(54,87)],"#FFCD68")]
        return r
    if kind=="helicopter":return [line("M 19 12 L 87 12 M 53 12 L 53 27",4),path("M 20 47 Q 17 21 54 27 Q 80 28 85 54 L 93 45 L 98 60 L 79 65 Q 62 78 27 65 Z","#FFCD68"),box(31,34,21,21,"#8DCDE5"),path("M 58 33 Q 73 35 78 54 L 59 54 Z","#8DCDE5"),line("M 30 66 L 28 84 M 66 67 L 68 84 M 15 85 L 79 85",3)]
    if kind=="hotairballoon":return [oval(50,34,32,31,"#F4A2BD"),path("M 43 4 Q 27 34 43 64 L 57 64 Q 73 34 57 4 Z","#FFCD68"),line("M 33 61 L 39 79 M 67 61 L 61 79",2),box(35,78,30,18,"#D9AD79")]
    if kind=="rocket":return [poly([(35,57),(17,78),(18,92),(40,80)],"#F8AA91"),poly([(65,57),(83,78),(82,92),(60,80)],"#F8AA91"),path("M 35 76 L 35 35 Q 38 12 50 3 Q 62 12 65 35 L 65 76 Z","#B9E3F0"),poly([(36,34),(50,4),(64,34)],"#F8AA91"),oval(50,46,10,color="#FFCD68"),box(35,70,30,10,"#BAA2E0"),path("M 40 82 L 44 96 L 50 90 L 56 96 L 60 82 Z","#FFCD68")]
    if kind in ["sailboat","ship","submarine","canoe"]:
        if kind=="submarine":return [box(13,40,76,40,"#FFCD68",20),box(44,27,21,15,"#FFCD68"),path("M 52 27 L 52 14 L 69 14 L 69 21 L 60 21 L 60 27 Z","#F8AA91"),*sum([wheel(x,59,9) for x in [32,55,77]],[]),poly([(13,49),(2,41),(2,77),(13,69)],"#F8AA91")]
        r=[path("M 5 66 L 94 66 L 80 89 Q 51 99 18 88 Z","#8DCDE5"),line("M 5 96 Q 20 87 35 96 Q 50 87 65 96 Q 80 87 95 96",1.5)]
        if kind=="sailboat":r += [box(48,12,4,54,"#D9AD79"),poly([(45,17),(11,60),(45,60)],"#FFCD68"),poly([(55,11),(84,58),(55,58)],"#F4A2BD")]
        elif kind=="ship":r += [box(18,39,66,26,"#F9F4DD"),box(32,23,37,16,"#BAA2E0"),box(50,10,12,13,"#F8AA91"),*sum([wheel(x,53,5) for x in [31,51,71]],[])]
        else:r += [line("M 24 29 L 71 79",4),oval(75,85,7,12,"#D9AD79")]
        return r
    if kind in ["train","tram"]:
        r=[box(16,26,70,45,"#8ACB9B"),*wheel(31,78,12),*wheel(72,78,12),box(27,34,46,22,"#8DCDE5"),line("M 10 93 L 93 93 M 10 98 L 93 98",2)]
        if kind=="train":r += [box(9,54,26,18,"#FFCD68"),box(19,13,12,29,"#F8AA91"),poly([(3,79),(16,65),(23,79)],"#F8AA91"),oval(52,65,5,color="#FFCD68")]
        else:r += [poly([(35,26),(48,9),(64,26)],"#BAA2E0"),line("M 22 9 L 78 9"),box(47,34,4,22,"#D9AD79")]
        return r
    # Road vehicles share wheels, with distinct bodies and functional parts.
    if kind in ["tractor","excavator","bulldozer","crane","cementmixer","dumptruck","garbagetruck"]:
        r=[*wheel(27,79,17),*wheel(75,81,13),box(15,49,70,24,"#FFCD68"),box(20,25,31,30,"#F8AA91"),box(24,30,22,19,"#8DCDE5")]
        if kind=="tractor":r += [box(54,42,34,24,"#8ACB9B"),box(74,25,5,18,"#A7B9CC"),line("M 63 50 L 63 59 M 70 50 L 70 59 M 77 50 L 77 59")]
        if kind=="excavator":r += [poly([(45,42),(60,13),(74,19),(83,46),(77,52),(67,29),(60,26),(53,47)],"#FFCD68"),path("M 77 45 L 94 47 L 99 63 L 79 63 Z","#F8AA91")]
        if kind=="bulldozer":r += [poly([(75,53),(93,48),(98,81),(78,83)],"#F8AA91"),box(7,70,65,19,"#A7B9CC",9)]
        if kind=="crane":r += [poly([(51,49),(65,4),(72,7),(61,53)],"#FFCD68"),line("M 68 8 L 90 8 L 90 51",2),line("M 85 51 Q 86 64 95 56",2)]
        if kind=="cementmixer":r += [path("M 50 25 L 72 21 Q 95 35 80 59 L 59 58 Q 41 49 50 25 Z","#BAA2E0"),line("M 56 27 L 77 54 M 65 23 L 84 44")]
        if kind=="dumptruck":r += [poly([(51,31),(96,22),(87,56),(54,60)],"#F8AA91"),line("M 64 32 L 68 51 M 76 29 L 79 48")]
        if kind=="garbagetruck":r += [path("M 49 24 L 88 24 Q 100 40 87 63 L 49 63 Z","#8ACB9B"),box(67,33,15,18,"#D6E7C5")]
        return r
    bus=kind in ["bus","schoolbus","camper","truck","firetruck","ambulance"]
    if bus:r=[box(7,31,86,43,"#FFCD68" if kind=="schoolbus" else "#F8AA91"),*wheel(25,77,13),*wheel(77,77,13),box(15,39,20,18,"#8DCDE5")]
    else:r=[path("M 8 61 L 21 52 L 33 29 L 68 29 L 83 52 L 94 59 L 94 76 L 8 76 Z","#8DCDE5"),*wheel(27,77,13),*wheel(77,77,13),poly([(28,51),(38,34),(52,34),(52,51)],"#DBEDF2"),poly([(57,34),(66,34),(78,51),(57,51)],"#DBEDF2")]
    if kind in ["bus","schoolbus"]:
        r += [box(x,39,13,18,"#8DCDE5") for x in [41,59,77]]
        r += [line("M 12 64 L 88 64"),box(8,23,85,8,"#FFCD68")]
        if kind=="schoolbus":r += [poly([(6,49),(0,53),(0,62),(6,66),(12,61),(12,54)],"#F8AA91")]
    if kind=="truck":r += [box(42,13,49,50,"#BAA2E0"),line("M 48 20 L 48 56 M 55 20 L 55 56")]
    if kind=="firetruck":r += [box(39,36,47,28,"#F8AA91"),box(17,18,69,9,"#FFCD68"),line("M 24 18 L 24 27 M 36 18 L 36 27 M 48 18 L 48 27 M 60 18 L 60 27 M 72 18 L 72 27"),oval(61,50,9,color="#A7B9CC")]
    if kind=="ambulance":r += [poly([(58,39),(69,39),(69,46),(77,46),(77,56),(69,56),(69,64),(58,64),(58,56),(50,56),(50,46),(58,46)],"#F8AA91"),box(38,21,26,10,"#8DCDE5")]
    if kind=="policecar":r += [box(41,20,23,9,"#F8AA91"),star(53,65,8,"#FFCD68")]
    if kind=="camper":r += [box(42,36,19,23,"#8DCDE5"),box(68,39,17,34,"#BAA2E0"),box(11,24,65,7,"#8ACB9B")]
    return r


def nature(kind):
    if kind in {"flower","sunflower","tulip","rose"}:return flower(kind)
    if kind=="tree":return [box(44,50,14,43,"#D9AD79"),path("M 25 68 C 3 67 3 39 23 35 C 17 11 44 2 57 16 C 80 0 99 25 84 42 C 105 67 66 79 56 65 Q 40 81 25 68 Z","#8ACB9B"),line("M 51 65 L 38 54 M 51 80 L 65 65")]
    if kind=="pinetree":return [box(43,72,14,23,"#D9AD79"),poly([(50,3),(20,36),(35,36),(10,66),(29,65),(3,86),(97,86),(71,65),(90,66),(65,36),(80,36)],"#8ACB9B")]
    if kind=="palmtree":return [path("M 44 92 Q 60 60 49 27 L 61 24 Q 72 59 59 92 Z","#D9AD79"),*sum([transform(leaf(50,38,25),0,0,1) if i==0 else [path(d,"#8ACB9B")] for i,d in enumerate(["","M 55 28 Q 4 8 7 52 Q 31 28 55 28 Z","M 55 27 Q 72 1 97 22 Q 74 24 55 27 Z","M 57 29 Q 99 24 98 58 Q 73 34 57 29 Z","M 51 25 Q 21 -1 25 8 L 51 25 Z"])],[]),oval(50,34,7,color="#C99A73"),oval(63,35,7,color="#C99A73")]
    if kind=="cactus":return [path("M 40 91 L 40 57 L 24 57 Q 13 55 13 43 L 13 29 Q 21 18 28 29 L 28 43 L 40 43 L 40 16 Q 51 0 62 16 L 62 55 L 73 55 L 73 33 Q 81 23 88 34 L 88 58 Q 87 69 62 70 L 62 91 Z","#8ACB9B"),line("M 49 20 L 49 80 M 22 33 L 22 46 M 80 40 L 80 56"),*transform(flower("tulip"),29,-5,.4)]
    if kind=="mushroom":return [path("M 37 43 L 34 85 Q 50 99 66 85 L 62 43 Z","#F9F4DD"),path("M 7 47 Q 12 4 50 5 Q 88 4 93 47 Q 50 60 7 47 Z","#F8AA91"),oval(33,30,10,8,"#FFCD68"),oval(65,23,8,7,"#F9F4DD"),*face(50,70,.9)]
    if kind=="sun":
        r=[poly([(50+31*math.cos(i*math.pi/4),50+31*math.sin(i*math.pi/4)),(50+46*math.cos(i*math.pi/4-.12),50+46*math.sin(i*math.pi/4-.12)),(50+46*math.cos(i*math.pi/4+.12),50+46*math.sin(i*math.pi/4+.12))],"#FFCD68") for i in range(8)]
        return r+[oval(50,50,29,color="#FFCD68"),*face(50,45,1.3)]
    if kind=="moon":return [path("M 72 5 C 14 2 0 85 62 96 Q 86 96 98 74 C 39 92 28 32 72 5 Z","#FFCD68"),*face(37,54,.9),star(84,21,10),star(82,48,7)]
    if kind=="star":return [star(50,51,45),*face(50,47,1.4)]
    if kind=="rainbow":return [path(f"M {7+i*8} 82 C {7+i*8} {2+i*13} {93-i*8} {2+i*13} {93-i*8} 82 L {85-i*8} 82 C {85-i*8} {18+i*13} {15+i*8} {18+i*13} {15+i*8} 82 Z",COLORS[i]) for i in range(4)]+cloud(17,78,.28)+cloud(83,78,.28)
    if kind=="cloud":return cloud(50,43,.9)+face(50,43,1.1)
    if kind=="raindrops":return cloud(50,31,.85)+[path(f"M {x} 64 Q {x-16} 84 {x} 91 Q {x+16} 84 {x} 64 Z","#8DCDE5") for x in [22,50,78]]
    if kind=="snowflake":
        r=[]
        for i in range(6):
            a=i*math.pi/3
            branch=[(0,-4),(20,-4),(25,-16),(31,-16),(27,-4),(43,-4),(43,4),(27,4),(31,16),(25,16),(20,4),(0,4)]
            r.append(poly([(50+x*math.cos(a)-y*math.sin(a),50+x*math.sin(a)+y*math.cos(a)) for x,y in branch],"#8DCDE5"))
        return r+[oval(50,50,8,color="#8DCDE5")]
    if kind in ["mountain","volcano","island","waterfall","beach","pond","garden"]:
        r=[]
        if kind in ["mountain","volcano"]:
            r += [poly([(5,90),(45,17),(94,90)],"#B8C6D5")]
            r += [poly([(31,42),(45,17),(63,43),(52,36),(45,42),(39,35)],"#F9F4DD")]
            if kind=="volcano":r += [path("M 37 29 L 53 30 Q 53 50 70 66 L 61 68 Q 48 62 47 45 Q 37 69 28 64 L 24 58 Q 38 45 37 29 Z","#F8AA91"),*cloud(47,8,.25)]
        elif kind=="waterfall":r += [poly([(7,84),(15,22),(91,20),(94,84)],"#C8BCA6"),path("M 36 22 L 65 22 Q 61 52 73 83 L 29 83 Q 43 53 36 22 Z","#8DCDE5"),oval(51,87,40,10,"#B9E3F0"),line("M 46 28 Q 51 56 43 73 M 57 30 Q 53 50 63 76")]
        elif kind=="island":r += [oval(50,84,46,11,"#8DCDE5"),oval(48,75,36,12,"#FFCD68"),*transform(nature("palmtree"),21,6,.67)]
        elif kind=="beach":r += [path("M 2 71 Q 40 52 98 66 L 98 96 L 2 96 Z","#FFCD68"),path("M 3 62 Q 40 44 97 54 L 97 69 Q 46 57 3 78 Z","#8DCDE5"),*transform(objects("umbrella"),10,2,.7),*transform(nature("shell"),65,71,.25)]
        elif kind=="pond":r += [oval(50,68,47,24,"#8DCDE5"),*transform(animal("frog"),7,35,.47),*transform(flower("tulip"),55,13,.5),line("M 56 73 Q 75 64 89 75")]
        else:r += [box(3,76,94,17,"#D9AD79"),*transform(flower(),4,18,.62),*transform(flower("tulip"),44,14,.7),*transform(animal("butterfly"),41,-1,.35)]
        return r
    if kind=="leaf":return leaf(50,50,41)
    if kind=="acorn":return [path("M 21 42 L 79 42 Q 77 78 50 94 Q 23 78 21 42 Z","#D9AD79"),path("M 14 44 Q 12 16 50 18 Q 88 16 86 44 Z","#C99A73"),box(47,6,8,15,"#8ACB9B"),line("M 27 29 L 37 40 M 43 26 L 54 40 M 61 26 L 71 40")]
    if kind=="pinecone":return [oval(50,53,30,39,"#D9AD79")]+[path(f"M {x} {y} Q {x+10} {y+18} {x+20} {y} Q {x+10} {y-8} {x} {y} Z","#D9AD79") for y in [28,45,62] for x in ([30,50] if y!=45 else [20,40,60])]
    if kind=="shell":return [path("M 50 88 Q 0 55 9 33 Q 14 19 27 23 Q 34 3 50 16 Q 66 3 73 23 Q 87 19 93 33 Q 100 55 50 88 Z","#F4A2BD"),line("M 50 86 L 27 24 M 50 86 L 50 17 M 50 86 L 73 24 M 50 86 L 11 36 M 50 86 L 91 36")]
    if kind=="coral":return [path("M 46 94 L 46 62 L 24 62 L 12 48 L 12 29 Q 20 20 27 30 L 27 43 L 46 48 L 46 13 Q 54 3 62 14 L 62 41 L 75 32 L 75 15 Q 84 7 90 17 L 90 39 L 62 62 L 62 94 Z","#F8AA91")]
    if kind=="nest":return [oval(50,71,43,21,"#D9AD79"),oval(36,56,13,18,"#B9E3F0"),oval(64,55,13,19,"#FFCD68"),path("M 7 64 Q 50 84 93 64 Q 85 94 50 96 Q 15 94 7 64 Z","#D9AD79"),line("M 19 77 L 64 91 M 39 78 L 80 85 M 18 86 L 72 77")]
    if kind=="planet":return [oval(50,49,30,color="#BAA2E0"),path("M 25 34 C -14 41 -1 84 54 74 C 117 62 109 15 74 28 L 81 34 C 107 31 86 66 48 66 C 15 72 -1 52 26 41 Z","#FFCD68"),oval(45,35,7,5,"#F4A2BD"),oval(62,49,5,8,"#8DCDE5"),star(86,9,5)]
    raise ValueError(kind)


def food(kind):
    if kind=="banana":return [path("M 19 9 Q 3 65 63 86 Q 90 92 94 62 Q 40 75 26 10 Z","#FFCD68"),line("M 23 17 Q 24 65 86 75"),box(15,3,14,10,"#D9AD79")]
    if kind in ["apple","pear","peach","mango","tomato","pepper"]:
        if kind=="pear":d="M 50 12 Q 68 9 65 33 C 65 48 98 62 83 82 C 63 105 13 92 16 65 Q 18 47 35 34 Q 35 15 50 12 Z"
        elif kind=="mango":d="M 41 21 C 69 -2 94 27 83 64 C 72 97 26 105 13 81 Q 5 62 27 44 Q 33 39 41 21 Z"
        elif kind=="pepper":d="M 25 21 Q 43 9 50 23 Q 70 9 83 26 Q 90 37 81 79 Q 69 102 53 88 Q 38 103 24 86 Q 10 47 25 21 Z"
        else:d="M 50 27 C 18 7 -3 39 16 74 C 27 100 42 87 50 91 C 65 103 84 86 91 63 C 103 26 71 9 50 27 Z"
        r=[path(d,"#F8AA91" if kind in ["apple","tomato","peach"] else "#FFCD68"),path("M 47 26 L 50 8 L 57 8 L 54 27 Z","#D9AD79"),*transform(leaf(65,15,14),0,0,1),*face(51,57,1.2)]
        if kind=="tomato":r += [poly([(50,29),(29,16),(43,17),(49,5),(57,17),(77,13),(64,29),(72,36),(53,32),(37,38)],"#8ACB9B")]
        if kind=="peach":r += [line("M 52 33 Q 70 62 54 86")]
        return r
    if kind in ["orange","lemon","kiwi","coconut","potato"]:
        r=[oval(50,55,37 if kind!="lemon" else 44,37 if kind!="lemon" else 27,"#FFCD68" if kind in ["orange","lemon"] else "#D9AD79"),*face(50,52,1.1)]
        if kind=="orange":r += leaf(60,15,16)
        if kind=="kiwi":r=[oval(50,52,39,38,"#D9AD79"),oval(50,52,31,31,"#8ACB9B"),oval(50,52,11,14,"#F9F4DD")]+[oval(50+20*math.cos(a),52+20*math.sin(a),1.5,2,INK,False,True) for a in [i*math.pi/6 for i in range(12)]]
        if kind=="coconut":r=[path("M 10 51 Q 50 5 90 51 Q 84 96 50 95 Q 15 94 10 51 Z","#D9AD79"),oval(50,51,39,16,"#F9F4DD"),oval(50,51,27,9,"#B9E3F0")]
        if kind=="potato":r += [oval(29,38,2,3,"#C99A73"),oval(66,77,3,2,"#C99A73"),oval(76,43,2,3,"#C99A73")]
        return r
    if kind=="strawberry":return [path("M 12 37 Q 10 13 49 25 Q 90 11 89 37 Q 86 70 50 94 Q 15 70 12 37 Z","#F8AA91"),poly([(21,21),(40,24),(51,6),(60,24),(82,19),(67,36),(53,31),(37,38)],"#8ACB9B")]+[oval(x,y,1.5,3,INK,False,True) for x,y in [(28,44),(49,44),(72,44),(37,61),(61,61),(50,78)]]
    if kind=="watermelon":return [path("M 5 29 L 95 29 Q 90 93 50 95 Q 10 93 5 29 Z","#8ACB9B"),path("M 13 29 L 87 29 Q 81 82 50 85 Q 19 82 13 29 Z","#F8AA91")]+[oval(x,y,2,3,INK,False,True) for x,y in [(25,42),(50,43),(75,42),(37,62),(63,62),(50,75)]]
    if kind=="pineapple":return [poly([(46,28),(26,5),(46,12),(53,0),(59,14),(80,7),(64,32)],"#8ACB9B"),oval(52,63,29,33,"#FFCD68"),line("M 27 46 L 70 85 M 24 62 L 57 94 M 37 34 L 80 73 M 76 44 L 33 84 M 81 60 L 46 95 M 63 32 L 24 72")]
    if kind=="grapes":return [oval(x,y,12,color="#BAA2E0") for x,y in [(30,35),(52,34),(73,37),(30,56),(53,56),(74,58),(42,76),(63,76),(53,91)]]+leaf(63,12,18)
    if kind=="cherries":return [line("M 25 62 Q 32 14 60 10 Q 73 33 73 62",3),oval(26,73,21,color="#F8AA91"),oval(73,73,21,color="#F4A2BD"),*leaf(71,16,16)]
    if kind=="avocado":return [path("M 50 6 Q 67 5 69 30 C 75 42 92 60 83 79 C 72 105 23 101 16 77 C 8 56 28 38 32 24 Q 34 7 50 6 Z","#8ACB9B"),path("M 50 17 Q 60 15 62 35 C 71 52 83 64 74 80 Q 50 98 27 78 Q 16 64 37 34 Q 38 17 50 17 Z","#DAEBA8"),oval(50,67,18,19,"#C99A73")]
    if kind=="carrot":return [poly([(49,25),(30,5),(49,14),(59,0),(61,18),(79,10),(66,31)],"#8ACB9B"),path("M 27 29 Q 53 15 74 32 Q 75 48 36 97 Q 20 59 27 29 Z","#F5BB73"),line("M 28 44 L 45 48 M 47 64 L 57 65 M 31 72 L 39 75")]
    if kind=="broccoli":return [path("M 41 93 L 40 61 L 19 47 L 25 38 L 48 53 L 64 36 L 74 43 L 59 65 L 61 93 Z","#A9CD8A"),path("M 17 58 C -1 57 3 34 19 33 C 16 12 43 7 49 23 C 64 3 88 18 82 34 C 104 40 93 66 74 59 C 64 77 44 67 45 60 Q 26 74 17 58 Z","#8ACB9B")]
    if kind=="corn":return [oval(50,48,22,43,"#FFCD68"),line("M 42 10 L 42 84 M 56 9 L 56 86 M 30 26 L 69 26 M 29 41 L 72 41 M 29 56 L 72 56 M 33 71 L 68 71"),path("M 49 95 Q 6 79 10 36 Q 40 47 49 95 Z","#8ACB9B"),path("M 49 95 Q 96 78 91 30 Q 60 51 49 95 Z","#AED4B0")]
    if kind=="pumpkin":return [oval(50,60,44,32,"#F5BB73"),oval(50,60,29,32,"#F8B565"),oval(50,60,13,32,"#FFCD68"),path("M 44 30 Q 49 9 61 9 L 64 18 Q 54 20 55 30 Z","#8ACB9B")]
    if kind=="peas":return [path("M 6 62 Q 36 1 95 36 Q 87 92 32 88 Z","#8ACB9B"),oval(31,60,13,color="#C2DFA3"),oval(56,51,14,color="#C2DFA3"),oval(80,44,12,color="#C2DFA3"),line("M 7 62 Q 47 80 96 36")]
    if kind=="eggplant":return [path("M 66 16 Q 84 25 74 55 Q 66 98 27 94 Q -1 84 20 59 Q 42 48 50 25 Z","#BAA2E0"),poly([(49,25),(45,10),(60,15),(69,4),(74,17),(88,18),(77,31),(64,23)],"#8ACB9B")]
    if kind=="bread":return [path("M 13 42 C -4 18 27 5 50 13 C 81 2 103 23 87 42 L 85 89 L 15 89 Z","#D9AD79"),path("M 23 43 C 7 23 31 17 50 23 C 73 14 91 27 77 43 L 75 80 L 25 80 Z","#F9E2B3"),*face(50,53,1.3)]
    if kind=="cheese":return [poly([(12,53),(69,19),(92,37),(91,87),(12,87)],"#FFCD68"),poly([(12,53),(69,19),(92,37),(38,61)],"#FBE3A0"),oval(32,73,7,color="#F5BB73"),oval(69,61,8,color="#F5BB73"),oval(78,80,5,color="#F5BB73")]
    if kind=="egg":return [path("M 50 4 C 73 4 94 57 84 79 C 75 101 25 101 16 79 C 6 57 27 4 50 4 Z","#F9F4DD"),*face(50,53,1.4)]
    if kind=="cupcake":return [poly([(19,54),(81,54),(73,93),(28,93)],"#BAA2E0"),path("M 12 51 Q 5 36 24 31 Q 25 15 44 19 Q 45 0 60 14 Q 83 14 80 32 Q 101 40 88 53 Z","#F4A2BD"),oval(52,9,6,color="#F8AA91"),line("M 34 63 L 38 86 M 49 63 L 50 86 M 65 63 L 62 86")]
    if kind=="icecream":return [poly([(22,50),(78,50),(50,98)],"#D9AD79"),line("M 29 62 L 64 74 M 37 76 L 56 86 M 70 62 L 37 76 M 62 76 L 44 88"),oval(50,31,30,27,"#F4A2BD"),path("M 19 43 Q 27 50 32 45 Q 38 67 46 47 Q 57 67 64 46 Q 75 56 81 43 Z","#F4A2BD")]
    if kind=="pizza":return [poly([(10,75),(69,7),(95,88)],"#FFCD68"),path("M 9 74 Q 48 73 96 88 L 94 98 Q 50 83 3 84 Z","#D9AD79"),oval(59,35,8,color="#F8AA91"),oval(36,65,7,color="#F8AA91"),oval(77,69,8,color="#F8AA91"),*transform(leaf(58,63,8),0,0,1)]
    raise ValueError(kind)


def objects(kind):
    if kind=="ball":return [oval(50,50,43,color="#FFCD68"),path("M 9 37 Q 50 9 91 37 L 91 63 Q 50 91 9 63 Z","#8DCDE5"),line("M 50 7 Q 21 50 50 93 M 50 7 Q 79 50 50 93")]
    if kind=="balloon":return [oval(50,34,29,31,"#F4A2BD"),poly([(50,64),(43,73),(57,73)],"#F4A2BD"),line("M 50 73 C 18 80 83 83 47 98"),*face(50,30,1.1)]
    if kind=="kite":return [poly([(50,3),(90,39),(50,73),(10,39)],"#FFCD68"),poly([(50,3),(90,39),(50,39)],"#F4A2BD"),poly([(50,39),(50,73),(10,39)],"#8DCDE5"),line("M 50 73 Q 79 81 53 98"),poly([(64,81),(54,76),(55,88)],"#BAA2E0"),poly([(66,89),(75,83),(73,96)],"#8ACB9B")]
    if kind=="robot":return [box(23,12,54,37,"#8DCDE5"),box(29,53,42,31,"#FFCD68"),box(9,54,16,29,"#BAA2E0"),box(75,54,16,29,"#BAA2E0"),box(29,86,16,12,"#8DCDE5"),box(55,86,16,12,"#8DCDE5"),line("M 50 12 L 50 5"),oval(50,4,3,color="#F8AA91"),*face(50,27,1.1),oval(42,66,4,color="#F8AA91"),oval(58,66,4,color="#8ACB9B")]
    if kind=="teddybear":return animal("bear")+[box(36,60,28,9,"#F4A2BD")]
    if kind=="doll":return [oval(50,30,23,25,"#D9AD79"),oval(50,32,18,19,"#F8D7B5"),poly([(38,50),(62,50),(79,82),(21,82)],"#F4A2BD"),box(31,81,12,16,"#F8D7B5"),box(57,81,12,16,"#F8D7B5"),oval(22,60,15,6,"#F8D7B5"),oval(78,60,15,6,"#F8D7B5"),*face(50,30,.75)]
    if kind in ["blocks","buildingbricks"]:
        if kind=="blocks":return [box(7,57,40,39,"#8DCDE5"),box(53,57,40,39,"#F4A2BD"),poly([(50,7),(24,51),(76,51)],"#FFCD68"),oval(27,76,10,color="#F9F4DD"),star(74,76,12,"#F9F4DD")]
        return [box(4,52,89,40,"#8DCDE5"),box(12,42,17,10,"#8DCDE5"),box(39,42,17,10,"#8DCDE5"),box(68,42,17,10,"#8DCDE5"),box(30,12,53,29,"#F8AA91"),box(38,3,14,9,"#F8AA91"),box(63,3,14,9,"#F8AA91"),line("M 48 54 L 48 91")]
    if kind=="top":return [path("M 12 47 Q 50 15 88 47 Q 73 75 50 89 Q 27 75 12 47 Z","#BAA2E0"),oval(50,47,38,12,"#FFCD68"),box(46,9,8,26,"#F8AA91"),line("M 15 92 Q 35 99 49 96 M 65 95 Q 84 92 88 82")]
    if kind=="yoyo":return [oval(46,60,36,color="#F4A2BD"),oval(46,60,24,color="#FFCD68"),oval(46,60,8,color="#BAA2E0"),line("M 46 59 C 92 66 89 5 60 7 Q 42 9 57 18",2)]
    if kind in ["drum","tambourine"]:
        if kind=="tambourine":return [oval(50,50,42,color="#D9AD79"),oval(50,50,29,color="#F9F4DD")]+[oval(50+35*math.cos(i*math.pi/3),50+35*math.sin(i*math.pi/3),6,color="#FFCD68") for i in range(6)]
        return [box(12,32,76,51,"#F8AA91"),oval(50,32,38,13,"#F9F4DD"),line("M 13 43 L 28 78 L 44 46 L 60 78 L 77 44 L 88 76",2),line("M 7 9 L 42 25 M 93 9 L 58 25",3),oval(40,24,5,color="#FFCD68"),oval(60,24,5,color="#FFCD68")]
    if kind=="xylophone":return [box(11,25,78,58,"#D9AD79")]+[box(18+i*11,15+i*3,9,72-i*6,COLORS[i%6],2) for i in range(6)]+[line("M 3 7 L 47 40",3),oval(48,41,6,color="#FFCD68")]
    if kind=="guitar":return [path("M 42 37 C 8 22 1 62 24 69 C 0 97 64 109 75 80 C 86 54 64 42 58 43 Z","#F8AA91"),box(45,11,11,46,"#D9AD79"),box(40,1,21,17,"#FFCD68"),oval(48,67,12,color="#F9F4DD"),line("M 48 16 L 48 86 M 52 16 L 52 85"),box(34,84,27,6,"#D9AD79")]
    if kind=="maraca":return [poly([(41,53),(57,50),(78,91),(64,98)],"#D9AD79"),oval(43,34,27,31,"#FFCD68"),path("M 17 25 Q 42 42 70 25 L 70 42 Q 43 57 18 43 Z","#F4A2BD"),star(44,26,9,"#8DCDE5")]
    if kind=="toytrain":return [*transform(vehicle("train"),0,5,.8),box(73,44,24,31,"#BAA2E0"),*wheel(79,82,8),*wheel(94,82,7),line("M 66 76 L 76 76")]
    if kind=="rockinghorse":return [path("M 8 82 Q 50 106 92 82 L 88 93 Q 50 116 12 93 Z","#D9AD79"),*transform(animal("horse"),8,-2,.84)]
    if kind=="boat_toy":return [*transform(vehicle("sailboat"),10,-1,.8),box(5,88,90,10,"#8DCDE5"),oval(11,81,5,color="#FFCD68")]
    if kind=="puzzle":return [path("M 8 8 L 40 8 C 25 33 69 33 55 8 L 92 8 L 92 42 C 67 28 67 70 92 56 L 92 92 L 57 92 C 71 67 28 67 42 92 L 8 92 L 8 56 C 32 70 32 27 8 42 Z","#BAA2E0"),star(51,50,17,"#FFCD68")]
    if kind in ["house","castle","toycastle","lighthouse","windmill","barn","tent"]:
        if kind=="tent":return [poly([(50,7),(97,91),(3,91)],"#FFCD68"),poly([(50,22),(73,90),(27,90)],"#8DCDE5"),line("M 50 22 L 50 90 M 12 79 L 2 97 M 88 79 L 98 97",2)]
        if kind in ["castle","toycastle"]:return [box(7,30,23,62,"#BAA2E0"),box(70,30,23,62,"#BAA2E0"),box(30,52,40,40,"#F4A2BD"),poly([(4,30),(18,7),(33,30)],"#8DCDE5"),poly([(67,30),(82,7),(97,30)],"#8DCDE5"),path("M 40 92 L 40 76 Q 50 58 60 76 L 60 92 Z","#FFCD68"),box(14,42,9,13,"#B9E3F0"),box(77,42,9,13,"#B9E3F0"),*([star(50,48,13),line("M 83 7 L 83 0 L 96 4 L 83 7")] if kind=="toycastle" else [box(39,37,22,15,"#FFCD68")])]
        if kind=="lighthouse":return [poly([(31,90),(40,29),(60,29),(69,90)],"#F9F4DD"),poly([(37,46),(64,46),(66,61),(35,61)],"#F8AA91"),box(33,17,34,17,"#8DCDE5"),poly([(29,17),(50,2),(71,17)],"#F8AA91"),box(44,73,12,17,"#D9AD79"),line("M 4 25 L 27 20 M 73 20 L 96 25")]
        if kind=="windmill":return [poly([(28,93),(37,29),(64,29),(75,93)],"#FFCD68"),poly([(32,29),(50,7),(69,29)],"#F8AA91"),box(43,74,14,19,"#D9AD79"),poly([(50,40),(13,9),(7,23),(50,48)],"#B9E3F0"),poly([(50,42),(82,8),(94,19),(55,49)],"#B9E3F0"),poly([(50,45),(93,73),(82,88),(46,51)],"#B9E3F0"),poly([(49,46),(17,89),(4,76),(44,42)],"#B9E3F0"),oval(50,46,6,color="#D9AD79")]
        if kind=="barn":return [box(12,35,76,58,"#F8AA91"),poly([(6,37),(26,13),(74,13),(94,37)],"#D9AD79"),box(32,53,36,40,"#FFCD68"),line("M 33 54 L 67 92 M 67 54 L 33 92"),oval(50,31,10,color="#B9E3F0")]
        return [box(15,40,70,53,"#FFCD68"),poly([(6,42),(50,5),(94,42)],"#F8AA91"),box(40,62,20,31,"#BAA2E0"),box(23,51,13,16,"#8DCDE5"),box(65,51,13,16,"#8DCDE5"),oval(55,80,1.5,color=INK,region=False,ink=True)]
    if kind=="umbrella":return [path("M 4 45 C 12 -7 88 -7 96 45 Q 78 33 66 45 Q 50 33 34 45 Q 20 33 4 45 Z","#F4A2BD"),path("M 34 45 Q 32 10 50 6 Q 68 10 66 45 Q 50 33 34 45 Z","#FFCD68"),line("M 50 44 L 50 85 Q 41 104 32 87",3)]
    if kind=="boots":return [path("M 13 9 L 43 9 L 43 70 L 49 72 L 49 93 L 9 93 L 9 77 L 13 72 Z","#FFCD68"),path("M 58 9 L 88 9 L 88 70 Q 101 73 98 93 L 54 93 L 54 77 L 58 72 Z","#8DCDE5"),box(13,12,30,9,"#F4A2BD"),box(58,12,30,9,"#BAA2E0"),star(28,48,10),oval(74,49,10,color="#FFCD68")]
    if kind=="hat":return [path("M 25 69 L 29 19 Q 50 4 71 19 L 76 69 Z","#BAA2E0"),oval(50,71,46,17,"#FFCD68"),path("M 27 55 L 74 55 L 76 67 L 25 67 Z","#F4A2BD")]
    if kind=="backpack":return [path("M 35 20 Q 32 0 50 2 Q 69 0 65 20 L 59 20 Q 60 9 50 9 Q 41 9 41 20 Z","#D9AD79"),box(15,18,70,76,"#8DCDE5",14),box(25,57,50,28,"#F4A2BD",8),star(50,39,12),line("M 25 64 L 75 64")]
    if kind=="book":return [path("M 50 20 Q 27 2 4 18 L 4 88 Q 27 72 50 90 Q 73 72 96 88 L 96 18 Q 73 2 50 20 Z","#B9E3F0"),line("M 50 21 L 50 88 M 15 35 Q 29 29 40 35 M 15 47 Q 29 41 40 47 M 60 58 Q 75 52 86 58 M 60 70 Q 75 64 86 70"),star(73,36,12,"#FFCD68")]
    if kind in ["pencil","crayons"]:
        pencil=[poly([(31,16),(60,4),(87,74),(69,98),(42,86)],"#FFCD68"),poly([(42,86),(69,98),(87,74),(74,76),(67,88),(53,83)],"#D9AD79"),poly([(69,98),(74,88),(79,90)],"#A7B9CC"),line("M 44 15 L 67 77"),poly([(31,16),(25,3),(54,-8),(60,4)],"#F4A2BD")]
        if kind=="pencil":return transform(pencil,5,9,.9)
        r=[]
        for i in range(4):r += [box(8+i*23,25-i*5,18,63,COLORS[i]),poly([(8+i*23,25-i*5),(17+i*23,6-i*2),(26+i*23,25-i*5)],COLORS[i]),box(8+i*23,61,18,14,"#F9F4DD")]
        return r
    if kind=="paintpalette":return [path("M 81 17 C 44 -14 1 26 6 64 C 10 103 64 99 59 79 C 48 59 106 58 81 17 Z","#D9AD79"),oval(72,38,10,7,"#F9F4DD")]+[oval(x,y,9,color=c) for x,y,c in [(31,25,"#F8AA91"),(52,20,"#FFCD68"),(22,48,"#8DCDE5"),(29,72,"#8ACB9B")]]+[poly([(52,90),(85,48),(92,53),(58,95)],"#BAA2E0")]
    if kind=="wateringcan":return [box(23,37,49,47,"#8ACB9B",8),path("M 27 38 Q 19 6 54 12 Q 69 17 63 38 L 55 38 Q 61 21 48 20 Q 30 17 35 38 Z","#FFCD68"),poly([(70,54),(85,29),(94,33),(77,78)],"#8ACB9B"),oval(91,29,10,5,"#FFCD68"),box(18,80,60,8,"#8ACB9B")]
    if kind=="bucket":return [path("M 23 37 Q 7 -2 50 4 Q 93 -2 77 37", "#D9AD79", False,3),poly([(14,33),(86,33),(75,92),(25,92)],"#FFCD68"),oval(50,34,36,8,"#8DCDE5"),star(50,63,15,"#F4A2BD")]
    if kind=="teapot":return [path("M 73 33 C 110 19 107 74 76 71 L 73 61 C 96 66 98 36 76 42 Z","#FFCD68"),poly([(26,49),(3,28),(5,53),(29,72)],"#8DCDE5"),oval(52,60,31,28,"#8DCDE5"),oval(52,33,26,6,"#F4A2BD"),oval(52,24,6,color="#FFCD68"),*face(52,55,1.1)]
    if kind=="cup":return [path("M 72 30 C 109 19 109 75 73 73 L 73 62 C 96 65 97 35 73 40 Z","#FFCD68"),path("M 15 24 L 77 24 L 71 77 Q 48 99 21 77 Z","#F4A2BD"),oval(46,24,31,8,"#F9F4DD"),*face(46,50,1.2)]
    if kind=="clock":return [oval(50,50,44,color="#FFCD68"),oval(50,50,35,color="#F9F4DD"),line("M 50 24 L 50 50 L 68 62",3),oval(50,50,4,color="#F8AA91")]+[oval(50+29*math.cos(i*math.pi/6),50+29*math.sin(i*math.pi/6),1.3,color=INK,region=False,ink=True) for i in range(12)]
    if kind=="gift":return [box(12,39,76,55,"#8DCDE5"),box(5,27,90,17,"#F4A2BD"),path("M 47 28 Q -1 -4 29 1 Q 48 1 50 25 Q 52 1 71 1 Q 101 -4 53 28 Z","#FFCD68"),box(43,28,14,66,"#FFCD68")]
    raise ValueError(kind)


ANIMALS = "bear:Osito,cat:Gato,dog:Perro,cow:Vaca,pig:Cerdo,sheep:Oveja,goat:Cabra,horse:Caballo,hen:Gallina,duck:Pato,bird:Pajarito,owl:Búho,lion:León,elephant:Elefante,monkey:Mono,frog:Rana,rabbit:Conejo,fox:Zorro,panda:Panda,koala:Koala,giraffe:Jirafa,zebra:Cebra,tiger:Tigre,crocodile:Cocodrilo,hippo:Hipopótamo,rhino:Rinoceronte,turtle:Tortuga,fish:Pez,dolphin:Delfín,whale:Ballena,shark:Tiburón,octopus:Pulpo,crab:Cangrejo,seahorse:Caballito de mar,penguin:Pingüino,butterfly:Mariposa,bee:Abeja,ladybug:Mariquita,snail:Caracol,hedgehog:Erizo"
DINOS = "brontosaurus:Brontosaurio,triceratops:Triceratops,stegosaurus:Estegosaurio,tyrannosaurus:Tiranosaurio,pteranodon:Pteranodon,ankylosaurus:Anquilosaurio,parasaurolophus:Parasaurolophus,spinosaurus:Espinosaurio,diplodocus:Diplodocus,velociraptor:Velociraptor,brachiosaurus:Braquiosaurio,apatosaurus:Apatosaurio,argentinosaurus:Argentinosaurio,styracosaurus:Estiracosaurio,protoceratops:Protoceratops,centrosaurus:Centrosaurio,kentrosaurus:Kentrosaurio,euoplocephalus:Euoplocéfalo,allosaurus:Alosaurio,carnotaurus:Carnotauro,pachycephalosaurus:Paquicefalosaurio,dilophosaurus:Dilofosaurio,iguanodon:Iguanodonte,pterodactyl:Pterodáctilo,quetzalcoatlus:Quetzalcoatlus"
VEHICLES = "car:Coche,bus:Autobús,truck:Camión,firetruck:Camión de bomberos,ambulance:Ambulancia,policecar:Coche de policía,tractor:Tractor,excavator:Excavadora,bulldozer:Topadora,crane:Grúa,cementmixer:Hormigonera,garbagetruck:Camión de basura,dumptruck:Volquete,train:Tren,tram:Tranvía,bicycle:Bicicleta,tricycle:Triciclo,scooter:Patinete,motorbike:Moto,airplane:Avión,helicopter:Helicóptero,hotairballoon:Globo aerostático,rocket:Cohete,sailboat:Velero,ship:Barco,submarine:Submarino,canoe:Canoa,spaceshuttle:Transbordador,camper:Caravana,schoolbus:Autobús escolar"
NATURE = "tree:Árbol,pinetree:Pino,palmtree:Palmera,flower:Flor,sunflower:Girasol,tulip:Tulipán,rose:Rosa,cactus:Cactus,mushroom:Seta,sun:Sol,moon:Luna,star:Estrella,rainbow:Arcoíris,cloud:Nube,raindrops:Lluvia,snowflake:Copo de nieve,mountain:Montaña,volcano:Volcán,island:Isla,waterfall:Cascada,leaf:Hoja,acorn:Bellota,pinecone:Piña de pino,shell:Concha,coral:Coral,nest:Nido,pond:Estanque,garden:Jardín,beach:Playa,planet:Planeta"
FOOD = "apple:Manzana,pear:Pera,banana:Plátano,orange:Naranja,lemon:Limón,strawberry:Fresa,watermelon:Sandía,pineapple:Piña,grapes:Uvas,cherries:Cerezas,peach:Melocotón,kiwi:Kiwi,avocado:Palta,mango:Mango,coconut:Coco,carrot:Zanahoria,tomato:Tomate,broccoli:Brócoli,corn:Maíz,pumpkin:Calabaza,pepper:Pimiento,peas:Guisantes,eggplant:Berenjena,potato:Patata,bread:Pan,cheese:Queso,egg:Huevo,cupcake:Magdalena,icecream:Helado,pizza:Pizza"
OBJECTS = "ball:Pelota,balloon:Globo,kite:Cometa,robot:Robot,teddybear:Peluche,doll:Muñeca,blocks:Bloques,top:Peonza,yoyo:Yoyó,drum:Tambor,xylophone:Xilófono,guitar:Guitarra,maraca:Maraca,tambourine:Pandereta,toytrain:Tren de juguete,rockinghorse:Caballito,boat_toy:Barquito,buildingbricks:Piezas de construcción,puzzle:Pieza de puzle,toycastle:Castillo de juguete,house:Casita,castle:Castillo,lighthouse:Faro,windmill:Molino,barn:Granero,tent:Tienda de campaña,umbrella:Paraguas,boots:Botas,hat:Sombrero,backpack:Mochila,book:Libro,pencil:Lápiz,crayons:Crayones,paintpalette:Paleta,wateringcan:Regadera,bucket:Cubo,teapot:Tetera,cup:Taza,clock:Reloj,gift:Regalo"


def build():
    DEST.mkdir(parents=True,exist_ok=True)
    pages=[]
    for category, label, subjects, maker in [("animals","Animales",ANIMALS,animal),("dinosaurs","Dinosaurios",DINOS,dinosaur),("vehicles","Vehículos",VEHICLES,vehicle),("nature","Naturaleza",NATURE,nature),("food","Frutas y alimentos",FOOD,food),("objects","Juguetes y objetos",OBJECTS,objects)]:
        for entry in subjects.split(","):
            key,title=entry.split(":")
            shapes=maker(key)
            # Several related dinosaur species have defining structural differences.
            # Add visible species-specific anatomy, not a recolor or reflected copy.
            if key=="protoceratops":shapes=[s for s in shapes if "76 11" not in s["d"] and "99 29" not in s["d"]]
            if key=="centrosaurus":shapes += [poly([(91,38),(97,15),(99,43)],"#FFCD68")]
            if key=="kentrosaurus":shapes += [poly([(18,70),(4,81),(19,78)],"#FFCD68"),poly([(8,59),(0,45),(15,64)],"#FFCD68")]
            if key=="euoplocephalus":shapes += [poly([(70,35),(69,22),(80,39)],"#FFCD68"),poly([(30,40),(23,26),(40,39)],"#FFCD68")]
            if key=="velociraptor":shapes += [poly([(30,91),(26,99),(45,98),(42,92)],"#FFCD68"),poly([(60,91),(57,99),(76,98),(72,92)],"#FFCD68")]
            if key=="allosaurus":shapes += [poly([(75,25),(76,17),(84,25)],"#FFCD68"),poly([(48,39),(51,27),(56,39)],"#FFCD68")]
            if key=="pterodactyl":shapes += [poly([(48,22),(40,4),(55,16)],"#F4A2BD")]
            # Related prehistoric animals receive different complete scenes.
            if key=="diplodocus":
                title="Diplodocus y palmera"
                shapes=transform(shapes,0,12,.79)+transform(nature("palmtree"),69,25,.32)
            if key=="brachiosaurus":
                title="Braquiosaurio comiendo hojas"
                shapes=transform(nature("tree"),70,0,.31)+transform(shapes,-1,9,.85)+leaf(84,29,7)
            if key=="apatosaurus":
                title="Apatosaurio en el estanque"
                shapes=[oval(50,85,47,13,"#B9E3F0"),*transform(shapes,2,0,.9),*transform(flower("tulip"),75,55,.22)]
            if key=="argentinosaurus":
                title="Familia de argentinosaurios"
                shapes=transform(shapes,-2,0,.78)+transform(dinosaur("brontosaurus"),58,57,.39)
            if key=="styracosaurus":
                title="Estiracosaurio y flores"
                shapes=transform(flower("sunflower"),0,3,.38)+transform(shapes,14,20,.78)+transform(flower("tulip"),-1,55,.4)
            if key=="centrosaurus":
                title="Centrosaurio y montañas"
                shapes=transform(nature("mountain"),8,0,.55)+transform(nature("mountain"),59,9,.4)+transform(shapes,3,28,.8)
            if key=="protoceratops":
                title="Protoceratops y su cría"
                shapes=transform(shapes,0,2,.75)+transform(dinosaur("protoceratops"),56,56,.4)
            if key=="euoplocephalus":
                title="Euoplocéfalo en el bosque"
                shapes=sum([transform(nature("pinetree"),x,0,z) for x,z in [(2,.45),(35,.34),(69,.3)]],[])+transform(shapes,4,30,.8)
            if key=="allosaurus":
                title="Alosaurio y su huevo"
                shapes=transform(shapes,0,8,.82)+transform(food("egg"),72,58,.27)
            if key=="pterodactyl":
                title="Pterodáctilo en la montaña"
                shapes=transform(nature("mountain"),40,47,.56)+transform(shapes,0,0,.8)
            if key=="quetzalcoatlus":
                title="Quetzalcoatlus entre nubes"
                shapes=cloud(15,17,.32)+cloud(81,70,.34)+transform(shapes,13,5,.77)
            pages.append({"id":"page_"+key,"name":title,"category":category,"categoryName":label,"layers":transform(shapes,100,25,4)})
        if category=="dinosaurs":
            scenes=[
                ("hatching_dino","Dinosaurio saliendo del huevo",[*transform(dinosaur("tyrannosaurus"),15,0,.7),path("M 13 51 L 28 60 L 40 46 L 54 61 L 67 46 L 83 59 L 94 49 Q 100 95 52 98 Q 7 99 13 51 Z","#FFCD68")]),
                ("dino_nest","Nido prehistórico",[*transform(nature("nest"),0,18,.98),*transform(dinosaur("triceratops"),3,-3,.5),*transform(leaf(),65,0,.32)]),
                ("dino_footprints","Huellas de dinosaurio",[poly([(x,y+22),(x-12,y+8),(x-12,y-5),(x-4,y+1),(x,y-14),(x+6,y),(x+15,y-8),(x+15,y+8)],"#D9AD79") for x,y in [(24,24),(69,51),(28,78)]]),
                ("dino_fossil","Fósil de dinosaurio",[oval(50,51,46,42,"#E8D9BB"),*transform(dinosaur("stegosaurus"),11,12,.77),line("M 32 55 L 56 55 M 38 48 L 38 65 M 49 48 L 49 65")]),
                ("dino_volcano","Dinosaurio y volcán",[*transform(nature("volcano"),53,0,.45),*transform(dinosaur("brontosaurus"),2,18,.77),*transform(nature("pinetree"),78,55,.23)]),
            ]
            for key,title,shapes in scenes:pages.append({"id":"page_"+key,"name":title,"category":category,"categoryName":label,"layers":transform(shapes,100,25,4)})
    assert len(pages)==200
    assert len({p["id"] for p in pages})==200
    hashes=[hashlib.sha256(json.dumps([(s["d"],s["region"],s["ink"]) for s in p["layers"]]).encode()).hexdigest() for p in pages]
    assert len(set(hashes))==200, "Every illustration must have different geometry"
    data={"version":1,"width":600,"height":450,"author":"Original vector artwork created for El mundo de Noah","pages":pages}
    (DEST/"catalog.json").write_text(json.dumps(data,ensure_ascii=False,separators=(",",":")),encoding="utf-8")
    print("Created 200 unique vector sheets:", {c:sum(p["category"]==c for p in pages) for c in sorted({p["category"] for p in pages})})


if __name__=="__main__":build()
