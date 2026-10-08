import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CaricaDocumento } from './carica-documento';

describe('CaricaDocumento', () => {
  let component: CaricaDocumento;
  let fixture: ComponentFixture<CaricaDocumento>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CaricaDocumento],
    }).compileComponents();

    fixture = TestBed.createComponent(CaricaDocumento);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
