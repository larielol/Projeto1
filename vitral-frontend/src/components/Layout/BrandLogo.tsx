import logo from '../../assets/vitral-sebo-icon.svg'
import './BrandLogo.css'

type BrandLogoProps = {
  size?: number
}

export function BrandLogo({ size = 166 }: BrandLogoProps) {
  return (
    <span
      className="vitral-logo"
      style={{
        '--logo-size': `${size}px`,
        backgroundImage: `url(${logo})`,
      } as React.CSSProperties}
      role="img"
      aria-label="Vitral"
    />
  )
}
