from abc import ABC, abstractmethod
from pathlib import Path

BASE_PROMPT_PATH = Path(__file__).parent / "prompt"


def _load_prompt(file_name: str) -> str:
    """(Helper) prompts 디렉터리에서 .txt 파일을 읽어옵니다."""
    try:
        with open(BASE_PROMPT_PATH / file_name, encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError as e:
        raise RuntimeError(f"Prompt file not found: {file_name}") from e
    except Exception as e:
        raise RuntimeError(f"Error loading prompt {file_name}: {e}") from e


# [Strategy Interface]
class ImageStyleStrategy(ABC):
    """이미지 생성 스타일(화풍)에 대한 전략 인터페이스"""

    @abstractmethod
    def get_system_prompt(self) -> str:
        pass


# [Concrete Strategies]
class AmericanComicsStrategy(ImageStyleStrategy):
    def __init__(self):
        self.prompt = _load_prompt("image_american_comics.txt")

    def get_system_prompt(self) -> str:
        return self.prompt


class NaturalStrategy(ImageStyleStrategy):
    def __init__(self):
        self.prompt = _load_prompt("image_natural.txt")

    def get_system_prompt(self) -> str:
        return self.prompt


class WatercolorStrategy(ImageStyleStrategy):
    def __init__(self):
        self.prompt = _load_prompt("image_watercolor.txt")

    def get_system_prompt(self) -> str:
        return self.prompt


class ThreeDAnimationStrategy(ImageStyleStrategy):
    def __init__(self):
        self.prompt = _load_prompt("image_3d_animation.txt")

    def get_system_prompt(self) -> str:
        return self.prompt


class PixelArtStrategy(ImageStyleStrategy):
    def __init__(self):
        self.prompt = _load_prompt("image_pixel_art.txt")

    def get_system_prompt(self) -> str:
        return self.prompt


class DefaultStrategy(ImageStyleStrategy):
    def get_system_prompt(self) -> str:
        return "Create an image that matches the diary content."


# [Factory Class]
class ImageStyleFactory:
    """요청된 스타일에 맞는 전략 객체를 생성하여 반환하는 팩토리"""

    def __init__(self):
        self._strategies = {
            "american-comics": AmericanComicsStrategy,
            "natural": NaturalStrategy,
            "watercolor": WatercolorStrategy,
            "3d-animation": ThreeDAnimationStrategy,
            "pixel-art": PixelArtStrategy,
        }

    def create_strategy(self, style: str) -> ImageStyleStrategy:
        strategy_class = self._strategies.get(style)
        if not strategy_class:
            return DefaultStrategy()
        return strategy_class()
