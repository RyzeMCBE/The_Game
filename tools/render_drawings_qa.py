"""Optional authoring QA: pip install cairosvg pillow; not needed for APK builds."""
from pathlib import Path
import argparse
import io
import json
import math
import html
import cairosvg
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]


def svg(page, colored=False):
    paths=[]
    for s in page["layers"]:
        fill=s["color"] if s["ink"] or (s["region"] and colored) else "white" if s["region"] else "none"
        paths.append(f'<path d="{html.escape(s["d"])}" fill="{fill}" stroke="{s["color"] if not s["region"] else "#243D59"}" stroke-width="{s["width"]}" stroke-linecap="round" stroke-linejoin="round"/>')
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450"><rect width="600" height="450" fill="white"/>'+''.join(paths)+'</svg>'


def main():
    p=argparse.ArgumentParser();p.add_argument("destination",type=Path);a=p.parse_args();a.destination.mkdir(parents=True,exist_ok=True)
    pages=json.loads((ROOT/"app/src/main/assets/drawings/catalog.json").read_text())["pages"]
    font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",14)
    for category in sorted({p["category"] for p in pages}):
        selected=[p for p in pages if p["category"]==category]
        contact=Image.new("RGB",(1200,math.ceil(len(selected)/5)*205),"#edf3f6");draw=ImageDraw.Draw(contact)
        for i,page in enumerate(selected):
            data=cairosvg.svg2png(bytestring=svg(page).encode(),output_width=230,output_height=173)
            x=(i%5)*240;y=(i//5)*205
            contact.paste(Image.open(io.BytesIO(data)).convert("RGB"),(x+5,y+3))
            draw.text((x+5,y+179),page["name"],font=font,fill="#243D59")
        contact.save(a.destination/(category+".png"))
    print("Rendered all 200 sheets for visual inspection")


if __name__=="__main__":main()
